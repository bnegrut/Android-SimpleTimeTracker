package com.example.util.simpletimetracker.domain.extension

/**
 * Adds item if it not in the list, otherwise removes it from the list.
 */
fun <T> MutableList<T>.addOrRemove(item: T) {
    if (item in this) remove(item) else add(item)
}

fun <T> MutableSet<T>.addOrRemove(item: T) {
    if (item in this) remove(item) else add(item)
}

operator fun <T> MutableCollection<in T>.plusAssign(element: T?) {
    if (element != null) this.add(element)
}