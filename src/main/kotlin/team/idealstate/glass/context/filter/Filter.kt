package team.idealstate.glass.context.filter

@FunctionalInterface
interface Filter<T> {

    fun filter(t: T): Boolean
}