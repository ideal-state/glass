package team.idealstate.glass.context.filter

@FunctionalInterface
interface Exclude<T>: Filter<T> {
    override fun filter(t: T): Boolean {
        return !exclude(t)
    }

    fun exclude(t: T): Boolean
}