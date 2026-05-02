package macos.hierarchy

data class ViewHierarchy(
    val root: AXNode? = null,
)

data class AXNode(
    val type: String? = null,
    val label: String? = null,
    val identifier: String? = null,
    val frame: AXRect? = null,
    val children: List<AXNode>? = null,
)

data class AXRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)
