const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write parties or agents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("parties").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("agents").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("parties").doc("p1").set({
      userId: ALICE_UID,
      name: "Walk-in",
      partyType: "CUSTOMER",
      openingBalance: 0,
      currentBalance: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Owner can create and read their own party, item, and agent", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("parties").doc("p1").set({
      userId: ALICE_UID,
      name: "Sharma Traders",
      partyType: "CUSTOMER",
      phone: "9876543210",
      stateName: "Maharashtra",
      openingBalance: 0,
      currentBalance: 0,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("items").doc("i1").set({
      userId: ALICE_UID,
      name: "Basmati Rice 5KG",
      itemType: "PRODUCT",
      unit: "BAG",
      salePrice: 650,
      purchasePrice: 520,
      taxRatePercent: 5,
      commissionPercent: 2.5,
      currentStock: 20,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("agents").doc("a1").set({
      userId: ALICE_UID,
      name: "Raju Mistri",
      agentType: "MISTRI",
      phone: "9988776655",
      defaultPercent: 3.0,
      defaultBasis: "TAXABLE",
      defaultTrigger: "ON_INVOICE",
      openingBalance: 0,
      currentBalance: 0,
      totalEarned: 0,
      totalPaid: 0,
      isActive: true,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("agents").get());
});

test("Cross-user access is denied (Bob cannot read or mutate Alice's items or agents)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const now = new Date();
    await context.firestore().collection("users").doc(ALICE_UID).collection("agents").doc("a1").set({
      userId: ALICE_UID,
      name: "Secret Mistri",
      agentType: "MISTRI",
      openingBalance: 0,
      currentBalance: 500,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("agents").doc("a1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("agents").get());
});

test("Shadow update with ghost field or updating cancelled transaction is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("parties").doc("p_ghost").set({
      userId: ALICE_UID,
      name: "Ghost Party",
      partyType: "CUSTOMER",
      openingBalance: 0,
      currentBalance: 0,
      ghostField: "hack",
      createdAt: now,
      updatedAt: now,
    })
  );

  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx1");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      txType: "SALE",
      refNumber: "INV/26-27/0001",
      dateMillis: Date.now(),
      isInterState: false,
      cgstAmount: 25,
      sgstAmount: 25,
      igstAmount: 0,
      taxAmount: 50,
      totalAmount: 550,
      paidAmount: 0,
      balanceAmount: 550,
      status: "UNPAID",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    txRef.update({
      status: "CANCELLED",
      cancelReason: "Customer changed mind",
      cancelledAtMillis: Date.now(),
      updatedAt: new Date(),
    })
  );
  await assertFails(txRef.update({ paidAmount: 550, balanceAmount: 0, status: "PAID", updatedAt: new Date() }));
});

test("Transactions stay immutable while profile exists, but can be wiped after profile deletion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID).collection("settings").doc("profile");
  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      businessName: "Alice Mart",
      currencySymbol: "₹",
      nextInvoiceNumber: 1,
      themeStyle: "DYNAMIC",
      stopNegativeStock: false,
      roundOffTotal: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx2");
  await assertSucceeds(
    txRef.set({
      userId: ALICE_UID,
      txType: "EXPENSE",
      refNumber: "EXP-101",
      dateMillis: Date.now(),
      totalAmount: 250,
      paidAmount: 250,
      balanceAmount: 0,
      status: "PAID",
      createdAt: now,
      updatedAt: now,
    })
  );
  // While profile exists, transaction cannot be deleted
  await assertFails(txRef.delete());

  // After owner deletes profile during account deletion, owner can wipe transactions
  await assertSucceeds(profileRef.delete());
  await assertSucceeds(txRef.delete());
});
