package be.geoffrey.kb;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.*;
import java.util.stream.Collectors;

public class SourceClass {

    private List<String> availableImplementationClassNames = new ArrayList<>();
    private String className;
    private String packageName;
    private List<Field> fields;
    private String superClassPackage;
    private String superClassName;
    private String sourceFileLocation;

    private SourceClass(String packageName, String className, List<Field> fields) {
        this.className = className;
        this.packageName = packageName;
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "SourceClass{" +
                "className='" + className + '\'' +
                ", packageName='" + packageName + '\'' +
                ", fields=" + fields +
                '}';
    }

    public String getFullyQualifiedClassName() {
        return packageName + "." + className;
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getClassName() {
        return className;
    }

    public String getNameFirstCharLowerCase() {
        return className.substring(0, 1).toLowerCase() + className.substring(1, className.length());
    }

    public String getNameAsLowerCase() {
        return className.toLowerCase();
    }

    public void addField(Field field) {
        fields.add(field);
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public static SourceClass fromJavaFile(CompilationUnit compilationUnit, String sourceFileLocation) {

        Optional<ClassOrInterfaceDeclaration> classDefinition = compilationUnit.getChildNodesByType(ClassOrInterfaceDeclaration.class)
                .stream()
                .findFirst();

        if (classDefinition.isPresent()) {

            List<Field> fieldNames = compilationUnit.getChildNodesByType(FieldDeclaration.class).stream()
                    .map(f -> {
                                final VariableDeclarator variableDeclaration = f.getVariables().get(0);

                                Field field;
                                if (hasTypeArguments(variableDeclaration)) {

                                    final String baseType = ((ClassOrInterfaceType) variableDeclaration.getType()).getName().toString();
                                    final List<String> types = ((ClassOrInterfaceType) variableDeclaration.getType()).getTypeArguments().get()
                                            .stream()
                                            .map(Node::toString)
                                            .collect(Collectors.toList());

                                    field = new Field(
                                            variableDeclaration.getName().toString(),
                                            variableDeclaration.getType().toString(),
                                            baseType,
                                            types
                                    );
                                } else {
                                    field = new Field(
                                            variableDeclaration.getName().toString(),
                                            variableDeclaration.getType().toString());

                                }


                                // Try to find a hibernate not null annotation
                                Optional<AnnotationExpr> columnAnnotation = f.getAnnotationByName("Column");
                                if (columnAnnotation.isPresent()) {
                                    for (MemberValuePair attribute : columnAnnotation.get().getChildNodesByType(MemberValuePair.class)) {
                                        if (attribute.getName().getIdentifier().equals("nullable")) {
                                            field.setMandatory(!((BooleanLiteralExpr) attribute.getValue()).getValue());
                                        }
                                    }
                                }

                                return field;
                            }
                    )
                    .collect(Collectors.toList());

            String packageName = compilationUnit.getPackageDeclaration().get().getName().toString();
            String className = classDefinition.get().getName().toString();

            SourceClass sourceClass = new SourceClass(packageName, className, fieldNames);
            sourceClass.setSourceFileLocation(sourceFileLocation);

            if (!classDefinition.get().getExtendedTypes().isEmpty()) {

                String superClassName = classDefinition.get().getExtendedTypes().get(0).getName().getIdentifier();
                sourceClass.setSuperClassName(superClassName);

                String superClassPackage = null;
                for (ImportDeclaration importDeclaration : compilationUnit.getImports()) {
                    if (importDeclaration.getNameAsString().endsWith(superClassName)) {
                        int index = importDeclaration.getNameAsString().lastIndexOf(".");
                        superClassPackage = importDeclaration.getNameAsString().substring(0, index);
                    }
                }
                if (superClassPackage == null) {
                    superClassPackage = packageName;
                }

                sourceClass.setSuperClassPackage(superClassPackage);

            }

            return sourceClass;

        } else {
            return null;
        }
    }

    private static String getPackage(ClassOrInterfaceType classOrInterfaceType) {
        return ((CompilationUnit) classOrInterfaceType.getParentNode().get().getParentNode().get()).getPackageDeclaration().get().getName().toString();
    }


    private static boolean hasTypeArguments(VariableDeclarator variableDeclaration) {
        return variableDeclaration.getType().getChildNodes().size() > 1;
    }

    public String getSuperClassPackage() {
        return superClassPackage;
    }

    public void setSuperClassPackage(String superClassPackage) {
        this.superClassPackage = superClassPackage;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public String getSuperClassFullyQualifiedName() {
        return superClassPackage + "." + superClassName;
    }

    public List<String> getAvailableImplementationClassNames() {
        return availableImplementationClassNames;
    }

    public void setAvailableImplementationClassNames(List<String> availableImplementationClassNames) {
        this.availableImplementationClassNames = availableImplementationClassNames;
    }

    public String getSourceFileLocation() {
        return sourceFileLocation;
    }

    public void setSourceFileLocation(String sourceFileLocation) {
        this.sourceFileLocation = sourceFileLocation;
    }
}