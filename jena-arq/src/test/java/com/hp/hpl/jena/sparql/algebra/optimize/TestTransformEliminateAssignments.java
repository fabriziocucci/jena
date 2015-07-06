/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.hpl.jena.sparql.algebra.optimize;

import org.apache.jena.atlas.lib.StrUtils;
import org.junit.Assert;
import org.junit.Test;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.sse.SSE;

/**
 * Tests for the {@link TransformEliminateAssignments}
 * 
 */
public class TestTransformEliminateAssignments {

    private void test(String input, String... output) {
        test(input, false, output);
    }

    private void test(String input, boolean aggressive, String... output) {
        Op original = SSE.parseOp(input);
        test(original, aggressive, output);
    }

    private void test(Op original, boolean aggressive, String... output) {
        // Transform
        Op actual = TransformEliminateAssignments.eliminate(original, aggressive);

        // Check results
        if (output == null) {
            // No transformation.
            Assert.assertEquals(original, actual);
        } else {
            // Transformation expected
            Op expected = SSE.parseOp(StrUtils.strjoinNL(output));
            Assert.assertEquals(expected, actual);
        }
    }

    private void testNoChange(String... input) {
        test(StrUtils.strjoinNL(input), (String[]) null);
    }

    @Test
    public void single_use_extend_01() {
        // Assigned variable used only once can substitute expression for the
        // later usage of the variable
        // However we must be inside a projection as otherwise the assigned
        // variable would be visible and we couldn't eliminate the assignment
        //@formatter:off
        test(StrUtils.strjoinNL("(project (?y)",
                                "  (filter (exprlist ?x)",
                                "    (extend (?x true)",
                                "      (table unit))))"),
             "(project (?y)",
             "  (filter (exprlist true)",
             "    (table unit)))");
        //@formatter:on
    }

    @Test
    public void single_use_extend_02() {
        // Assignment for ?y can be removed because it is never used
        // However we must be inside a projection as otherwise the assigned
        // variable would be visible and we couldn't eliminate the assignment
        //@formatter:off
        test(StrUtils.strjoinNL("(project (?z)",
                                "  (filter (exprlist ?x)",
                                "    (extend ((?x true) (?y false))",
                                "      (table unit))))"),
             "(project (?z)",
             "  (filter (exprlist true)",
             "    (table unit)))");
        //@formatter:on
    }

    @Test
    public void single_use_extend_03() {
        // Assigned variable used only once can substitute expression for the
        // later usage of the variable
        // However we must be inside a projection as otherwise the assigned
        // variable would be visible and we couldn't eliminate the assignment
        //@formatter:off
        test(StrUtils.strjoinNL("(project (?y)",
                                "  (order (?x)",
                                "    (extend (?x true)",
                                "      (table unit))))"),
             "(project (?y)",
             "  (order (true)",
             "    (table unit)))");
        //@formatter:on
    }

    @Test
    public void single_use_extend_complex_01() {
        // Assigned variable used only once can substitute expression for the
        // later usage of the variable
        // BUT we won't do this by default for complex expressions where they
        // are used in a place where they could be evaluated multiple times
        //@formatter:off
        testNoChange(StrUtils.strjoinNL("(project (?y)",
                                        "  (order (?x)",
                                        "    (extend (?x (contains 'foo' 'bar'))",
                                        "      (table unit))))"));
        //@formatter:on
    }

    @Test
    public void single_use_extend_complex_02() {
        // Assigned variable used only once can substitute expression for the
        // later usage of the variable
        // BUT we won't do this by default for complex expressions where they
        // are used in a place where they could be evaluated multiple times
        // EXCEPT if we are doing aggressive in-lining
        //@formatter:off
        test(StrUtils.strjoinNL("(project (?y)",
                                "  (order (?x)",
                                "    (extend (?x (contains 'foo' 'bar'))",
                                "      (table unit))))"),
             true,
             "(project (?y)",
             "  (order ((contains 'foo' 'bar'))",
             "    (table unit)))");
        //@formatter:on
    }
    
    @Test
    public void single_use_extend_unstable_01() {
        // Assigned variable used only once can substitute expression for the
        // later usage of the variable
        // EXCEPT if the expression is unstable in which case we leave it alone
        //@formatter:off
        testNoChange(StrUtils.strjoinNL("(project (?y)",
                                        "  (filter (exprlist ?x)",
                                        "    (extend (?x (rand))",
                                        "      (table unit))))"));
        //@formatter:on
    }
    
    @Test
    public void single_use_extend_unstable_02() {
        // Assigned variable used only once can substitute expression for the
        // later usage of the variable
        // EXCEPT if the expression is unstable in which case we leave it alone
        //@formatter:off
        testNoChange(StrUtils.strjoinNL("(project (?y)",
                                        "  (filter (exprlist ?x)",
                                        "    (extend (?x (uuid))",
                                        "      (table unit))))"));
        //@formatter:on
    }
    
    @Test
    public void single_use_extend_unstable_03() {
        // Assigned variable used only once can substitute expression for the
        // later usage of the variable
        // EXCEPT if the expression is unstable in which case we leave it alone
        //@formatter:off
        testNoChange(StrUtils.strjoinNL("(project (?y)",
                                        "  (filter (exprlist ?x)",
                                        "    (extend (?x (struuid))",
                                        "      (table unit))))"));
        //@formatter:on
    }
    
    @Test
    public void single_use_extend_unstable_04() {
        // Assigned variable used only once can substitute expression for the
        // later usage of the variable
        // EXCEPT if the expression is unstable in which case we leave it alone
        //@formatter:off
        testNoChange(StrUtils.strjoinNL("(project (?y)",
                                        "  (filter (exprlist ?x)",
                                        "    (extend (?x (bnode))",
                                        "      (table unit))))"));
        //@formatter:on
    }

    @Test
    public void single_use_extend_outside_projection_01() {
        // Cannot eliminate as there is no projection so the assigned variable
        // is visible even though in the algebra given it is used only once
        //@formatter:off
        testNoChange("(filter (exprlist ?x)",
                     "  (extend (?x true)",
                     "    (table unit)))");
        //@formatter:on
    }

    @Test
    public void single_use_extend_outside_projection_02() {
        // Cannot eliminate as there is no projection so the assigned variable
        // is visible even though in the algebra given it is used only once
        //@formatter:off
        testNoChange("(filter (exprlist ?x)",
                     "  (extend ((?x true) (?y false))",
                     "    (table unit)))");
        //@formatter:on
    }

    @Test
    public void multi_use_extend_01() {
        // As the assigned variable is used multiple times we leave the
        // assignment alone
        //@formatter:off
        testNoChange("(project (?y)",
                     "  (filter (> (* ?x ?x) 16)",
                     "    (extend (?x 3)",
                     "      (table unit))))");
        //@formatter:on
    }

    @Test
    public void multi_use_extend_02() {
        // Because the value of the assignment is used in multiple places we
        // leave the assignment alone
        //@formatter:off
        testNoChange("(project (?y)",
                     "  (filter (exprlist ?x)",
                     "    (join",
                     "      (extend (?x true)",
                     "        (table unit))",
                     "      (bgp (triple ?x ?y ?z)))))");
        //@formatter:on
    }

    @Test
    public void scoped_use_extend_01() {
        // If the assignment is out of scope by the time it is used in the outer
        // scope then we can't substitute it out there
        // However if the scoping means the value is never used we can instead
        // eliminate it entirely
        //@formatter:off
        test(StrUtils.strjoinNL("(filter (exprlist ?x)",
                                "  (project (?y)",
                                "    (extend (?x true)",
                                "      (table unit))))"),
            "(filter (exprlist ?x)",
            "  (project (?y)",
            "    (table unit)))");
        //@formatter:on
    }

    @Test
    public void scoped_use_extend_02() {
        // If the assignment is out of scope by the time it is used in the outer
        // scope then we can't substitute it out there
        // However in this case we can substitute it in the inner scope
        //@formatter:off
        test(StrUtils.strjoinNL("(filter (exprlist ?x)",
                                "  (project (?y)",
                                "    (filter (exprlist ?x)",
                                "      (extend (?x true)",
                                "        (table unit)))))"),
            "(filter (exprlist ?x)",
            "  (project (?y)",
            "    (filter (exprlist true)",
            "      (table unit))))");
        //@formatter:on
    }
}
