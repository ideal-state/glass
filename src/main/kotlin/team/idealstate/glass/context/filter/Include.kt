package team.idealstate.glass.context.filter

@FunctionalInterface
interface Include<T>: Filter<T> {
    override fun filter(it: T): Boolean {
        return include(it)
    }

    fun include(it: T): Boolean
}