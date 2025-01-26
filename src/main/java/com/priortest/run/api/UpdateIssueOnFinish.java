package com.priortest.run.api;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class UpdateIssueOnFinish {

    private static final Logger log = Logger.getLogger(UpdateIssueOnFinish.class.getName());

    public static void updateIssueIds(File javaFile, String methodName, String[] newIssueIds) {
        try {
            CompilationUnit cu;
            try {
                cu = StaticJavaParser.parse(javaFile);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (newIssueIds == null) {
                // remove issueIds
                cu.findAll(MethodDeclaration.class).forEach(method -> {
                    if (method.getNameAsString().equals(methodName)) {
                        method.getAnnotationByName("TestCaseApi").ifPresent(annotation -> {
                            annotation.ifNormalAnnotationExpr(normalAnnotation -> {
                                // Remove the `issueId` pair if it exists
                                normalAnnotation.getPairs().removeIf(pair -> pair.getNameAsString().equals("issueId"));
                            });
                        });
                    }
                });
            } else {
                // Locate the method and update the annotation
                // update issue list
                cu.findAll(MethodDeclaration.class).forEach(method -> {
                    if (method.getNameAsString().equals(methodName)) {
                        method.getAnnotationByName("TestCaseApi").ifPresent(annotation -> {
                            annotation.ifNormalAnnotationExpr(normalAnnotation -> {
                                normalAnnotation.getPairs().forEach(pair -> {
                                    if (pair.getNameAsString().equals("issueId")) {
                                        // If the issueId is an ArrayInitializerExpr, replace its values
                                        if (pair.getValue() instanceof ArrayInitializerExpr array) {
                                            // Use setValues instead of addValue to replace the values
                                            NodeList<Expression> newValues = new NodeList<>();
                                            for (String issueId : newIssueIds) {
                                                newValues.add(new StringLiteralExpr(issueId));
                                            }
                                            array.setValues(newValues);  // Set all new values
                                        } else if (pair.getValue() instanceof StringLiteralExpr existingIssueId) {
                                            // If it's a single StringLiteralExpr, convert it into an array and add new issue IDs
                                            // Create a new NodeList for the array initializer
                                            NodeList<Expression> newValues = new NodeList<>();
                                            // Add the existing value (if any)
                                            newValues.add(existingIssueId);
                                            // Add the new issue IDs
                                            for (String issueId : newIssueIds) {
                                                newValues.add(new StringLiteralExpr(issueId));
                                            }
                                            // Create a new ArrayInitializerExpr and set the values
                                            ArrayInitializerExpr newArray = new ArrayInitializerExpr();
                                            newArray.setValues(newValues);
                                            // Update the pair with the new array
                                            pair.setValue(newArray);
                                        }
                                    }
                                });
                            });
                        });

                        // If there is no annotation for 'issueId', we add it with an empty value
                        if (!method.getAnnotationByName("TestCaseApi").isPresent()) {
                            method.addAnnotation(new NormalAnnotationExpr(new Name("TestCaseApi"),  // Corrected: Directly use Name class
                                    new NodeList<>(new MemberValuePair("issueId", new ArrayInitializerExpr()))));
                        }
                    }
                });
            }
            // Write back the updated file
            try (FileWriter writer = new FileWriter(javaFile)) {
                writer.write(cu.toString());
            }
            log.info("Updated file: " + javaFile.getPath());
        } catch (IOException e) {
            log.severe("File writing error: " + e.getMessage());
        } catch (Exception e) {
            log.severe("Error updating issue IDs in file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
