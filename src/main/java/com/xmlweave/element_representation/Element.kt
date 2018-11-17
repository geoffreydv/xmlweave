package com.xmlweave.element_representation

data class Element(val name: String,
                   val children: List<Element> = ArrayList(),
                   val attributes: List<Attribute> = ArrayList(),
                   val value: String? = null,
                   var prefix: String? = null) {

    fun isLeaf(): Boolean {
        return children.isEmpty()
    }
}
