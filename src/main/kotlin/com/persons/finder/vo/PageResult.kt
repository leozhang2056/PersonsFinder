package com.persons.finder.vo

data class PageResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int
) {
    companion object {
        fun <T> of(items: List<T>, page: Int, size: Int, totalItems: Long): PageResult<T> {
            return PageResult(
                items = items,
                page = page,
                size = size,
                totalItems = totalItems,
                totalPages = if (size > 0) ((totalItems + size - 1) / size).toInt().coerceAtLeast(1) else 1
            )
        }
    }
}
