package be.geoffrey.kb;

import java.util.List;

public class Field {

    // TODO: Add a more reliable way of determining if a field is a collection

    private String name;
    private String fieldType;
    private boolean mandatory; // Tries to see if this field is mandatory using hibernate annotations
    private String baseType;
    private String typeArgument;

    public Field(String name, String fieldType) {
        this.name = name;
        this.fieldType = fieldType;
    }

    public Field(String name, String fieldType, String baseType, List<String> types) {
        this(name, fieldType);
        this.baseType = baseType;
        typeArgument = types.get(0);
    }

    public String getName() {
        return name;
    }

    public String getTypeArgument() {
        return typeArgument;
    }

    public String getBaseType() {
        return baseType;
    }

    public String getUpperCaseName() {
        return name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
    }

    public String getGetter() {
        String getter = "public " + fieldType + " get" + getUpperCaseName() + "() {\n";
        getter += "        return this." + name + ";\n";
        getter += "   }\n";
        return getter;
    }

    public String getSetter() {
        String getter = "public void set" + getUpperCaseName() + "(" + fieldType + " " + name + ") {\n";
        getter += "        this." + name + " = " + name + ";\n";
        getter += "    }\n";
        return getter;
    }

    public String getFieldType() {
        return fieldType;
    }

    public boolean isString() {
        return fieldType.equals("String") || fieldType.equals("java.lang.String");
    }

    public boolean isDate() {
        return fieldType.equals("LocalDate")
                || fieldType.equals("LocalDateTime");
    }

    public boolean isNumeric() {
        return fieldType.equals("int")
                || fieldType.equals("Integer")
                || fieldType.equals("double")
                || fieldType.equals("Double");
    }

    public boolean isCollection() {
        return fieldType.contains("Set") || fieldType.contains("List");
    }

    @Override
    public String toString() {
        return "Field{" +
                "name='" + name + '\'' +
                ", fieldType='" + fieldType + '\'' +
                '}';
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
}