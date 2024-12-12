package team.idealstate.glass.context.filter

@FunctionalInterface
interface Filter<T> {

    fun filter(it: T): Boolean
}