/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.flags;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/codegen/java8/writeFlags")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class WriteFlagsTestGenerated extends AbstractWriteFlagsTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
    }

    public void testAllFilesPresentInWriteFlags() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/java8/writeFlags"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
    }

    @TestMetadata("interfaceMethod.kt")
    public void testInterfaceMethod() throws Exception {
        runTest("compiler/testData/codegen/java8/writeFlags/interfaceMethod.kt");
    }

    @TestMetadata("interfaceProperty.kt")
    public void testInterfaceProperty() throws Exception {
        runTest("compiler/testData/codegen/java8/writeFlags/interfaceProperty.kt");
    }

    @TestMetadata("compiler/testData/codegen/java8/writeFlags/defaults")
    @TestDataPath("$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Defaults extends AbstractWriteFlagsTest {
        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
        }

        public void testAllFilesPresentInDefaults() throws Exception {
            KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/java8/writeFlags/defaults"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
        }

        @TestMetadata("defaultMethod.kt")
        public void testDefaultMethod() throws Exception {
            runTest("compiler/testData/codegen/java8/writeFlags/defaults/defaultMethod.kt");
        }

        @TestMetadata("defaultMethodInDiamond.kt")
        public void testDefaultMethodInDiamond() throws Exception {
            runTest("compiler/testData/codegen/java8/writeFlags/defaults/defaultMethodInDiamond.kt");
        }

        @TestMetadata("defaultProperty.kt")
        public void testDefaultProperty() throws Exception {
            runTest("compiler/testData/codegen/java8/writeFlags/defaults/defaultProperty.kt");
        }

        @TestMetadata("defaultPropertyInDiamond.kt")
        public void testDefaultPropertyInDiamond() throws Exception {
            runTest("compiler/testData/codegen/java8/writeFlags/defaults/defaultPropertyInDiamond.kt");
        }

        @TestMetadata("propertyAnnotation.kt")
        public void testPropertyAnnotation() throws Exception {
            runTest("compiler/testData/codegen/java8/writeFlags/defaults/propertyAnnotation.kt");
        }

        @TestMetadata("compiler/testData/codegen/java8/writeFlags/defaults/compatibility")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class Compatibility extends AbstractWriteFlagsTest {
            private void runTest(String testDataFilePath) throws Exception {
                KotlinTestUtils.runTest(this::doTest, TargetBackend.ANY, testDataFilePath);
            }

            public void testAllFilesPresentInCompatibility() throws Exception {
                KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/java8/writeFlags/defaults/compatibility"), Pattern.compile("^(.+)\\.kt$"), TargetBackend.ANY, true);
            }

            @TestMetadata("defaultMethodInDiamond.kt")
            public void testDefaultMethodInDiamond() throws Exception {
                runTest("compiler/testData/codegen/java8/writeFlags/defaults/compatibility/defaultMethodInDiamond.kt");
            }

            @TestMetadata("defaultPropertyInDiamond.kt")
            public void testDefaultPropertyInDiamond() throws Exception {
                runTest("compiler/testData/codegen/java8/writeFlags/defaults/compatibility/defaultPropertyInDiamond.kt");
            }

            @TestMetadata("propertyAccessors.kt")
            public void testPropertyAccessors() throws Exception {
                runTest("compiler/testData/codegen/java8/writeFlags/defaults/compatibility/propertyAccessors.kt");
            }

            @TestMetadata("propertyAnnotation.kt")
            public void testPropertyAnnotation() throws Exception {
                runTest("compiler/testData/codegen/java8/writeFlags/defaults/compatibility/propertyAnnotation.kt");
            }
        }
    }
}
