package team.idealstate.glass.context.filter

@FunctionalInterface
interface Include<T>: Filter<T> {
    override fun filter(t: T): Boolean {
        return include(t)
    }

    fun include(t: T): Boolean
}