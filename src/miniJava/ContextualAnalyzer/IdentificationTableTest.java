// package miniJava.ContextualAnalyzer;
//
// import static org.junit.Assert.fail;
//
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.Assert;
//
// public class IdentificationTableTest {
//
// IdentificationTable<String, String> idTable;
//
// @Before
// public void setUp() throws Exception {
// idTable = new IdentificationTable<String, String>();
// }
//
// @After
// public void tearDown() throws Exception {
// idTable = null;
// }
//
// @Test
// public final void testExceptionOnDuplicateInSameScopeLevel() {
// try {
// idTable.put("test", null);
// idTable.put("test", null);
// }
// catch (IdentifierError ie) {
// return;
// }
//
// fail();
// }
//
// @Test
// public final void testMemberNameShadowingClassName() {
// try {
// // Class level (1)
// idTable.openScope();
// idTable.put("a", null);
//
// // Member level (2)
// idTable.openScope();
// idTable.put("a", null);
// }
// catch (IdentifierError ie) {
// fail();
// }
// }
//
// @Test
// public final void testParamShadowingClassNameAndMemberName() {
// try {
// // Class level (1)
// idTable.openScope();
// idTable.put("a", null);
//
// // Member level (2)
// idTable.openScope();
// idTable.put("b", null);
//
// // Param level (3)
// idTable.openScope();
// idTable.put("b", null);
// }
// catch (IdentifierError ie) {
// fail();
// }
// }
//
// @Test
// public final void testExceptionOnLocalShadowingParamName() {
// try {
// // Class level (1)
// idTable.openScope();
// idTable.put("a", null);
//
// // Member level (2)
// idTable.openScope();
// idTable.put("b", null);
//
// // Param level (3)
// idTable.openScope();
// idTable.put("c", null);
//
// // Local level (4)
// idTable.openScope();
// idTable.put("c", null);
// }
// catch (IdentifierError ie) {
// return;
// }
//
// fail();
// }
//
// @Test
// public final void testExceptionOnLocalShadowingLocal() {
// try {
// // Class level (1)
// idTable.openScope();
// idTable.put("a", null);
//
// // Member level (2)
// idTable.openScope();
// idTable.put("b", null);
//
// // Param level (3)
// idTable.openScope();
// idTable.put("c", null);
//
// // Local level (4)
// idTable.openScope();
// idTable.put("d", null);
//
// // Local level (5)
// idTable.openScope();
// idTable.put("d", null);
// }
// catch (IdentifierError ie) {
// return;
// }
//
// fail();
// }
//
// @Test
// public void testSimpleGet() {
// try {
// String input = "value";
// idTable.put("key", input);
// String output = idTable.get("key");
// Assert.assertEquals(input, output);
// }
// catch (IdentifierError e) {
// fail();
// }
// }
//
// @Test
// public void testScopedGet() {
// try {
// // Class level (1)
// idTable.openScope();
// idTable.put("a", "aVal");
//
// // Member level (2)
// idTable.openScope();
// idTable.put("b", "bVal");
//
// Assert.assertEquals("bVal", idTable.get("b"));
// Assert.assertEquals("aVal", idTable.get("a"));
// }
// catch (IdentifierError e) {
// fail();
// }
// }
//
// @Test
// public void testShadowedGetMemberLevel() {
// try {
// // Class level (1)
// idTable.openScope();
// idTable.put("a", "aVal");
//
// // Member level (2)
// idTable.openScope();
// idTable.put("a", "bVal");
//
// Assert.assertEquals("bVal", idTable.get("a"));
// }
// catch (IdentifierError e) {
// fail();
// }
// }
//
// @Test
// public void testShadowedGetParamLevel() {
// try {
// // Class level (1)
// idTable.openScope();
// idTable.put("a", "aVal");
//
// // Member level (2)
// idTable.openScope();
// idTable.put("a", "bVal");
//
// // Param level (3)
// idTable.openScope();
// idTable.put("a", "cVal");
//
// Assert.assertEquals("cVal", idTable.get("a"));
// }
// catch (IdentifierError e) {
// fail();
// }
// }
//
// @Test
// public void testExceptionOnScopeCloseAtRootLevel() {
// try {
// idTable.closeScope();
// }
// catch (IdentifierError ie) {
// return;
// }
//
// fail();
// }
//
// @Test
// public void testCloseScope() {
// try {
// // Class level (1)
// idTable.openScope();
// idTable.put("a", "aVal");
//
// // Member level (2)
// idTable.openScope();
// idTable.put("b", "bVal");
// idTable.closeScope();
//
// idTable.get("b");
// }
// catch (IdentifierError e) {
// return;
// }
//
// fail();
// }
// }
