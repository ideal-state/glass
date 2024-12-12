package team.idealstate.glass.context.filter

@FunctionalInterface
interface Exclude<T>: Filter<T> {
    override fun filter(it: T): Boolean {
        return !exclude(it)
    }

    fun exclude(it: T): Boolean
}