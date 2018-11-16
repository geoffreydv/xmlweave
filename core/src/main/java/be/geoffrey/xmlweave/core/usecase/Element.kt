package be.geoffrey.xmlweave.core.usecase

data class Element(val name: String, val children: List<Element> = ArrayList(), val value: String? = null) {
    fun isLeaf(): Boolean {
        return children.isEmpty()
    }
}
