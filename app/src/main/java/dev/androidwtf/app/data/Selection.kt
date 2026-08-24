package dev.androidwtf.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/** What the user has picked, plus the filters applied to the catalogue view. */
class Selection {
    val picked: SnapshotStateList<String> = mutableStateListOf()

    var query = mutableStateOf("")
    var bundle = mutableStateOf<String?>(null)
    var maxTier = mutableStateOf<Int?>(null)
    var scriptableOnly = mutableStateOf(false)

    fun toggle(id: String) = if (id in picked) picked.remove(id) else picked.add(id)
    fun isPicked(id: String) = id in picked
    fun clear() = picked.clear()
    fun addAll(ids: List<String>) = ids.forEach { if (it !in picked) picked.add(it) }

    fun filter(all: List<Tool>): List<Tool> {
        val q = query.value.trim().lowercase()
        return all.filter { t ->
            (bundle.value == null || t.bundle == bundle.value) &&
                (maxTier.value == null || t.tier <= maxTier.value!!) &&
                (!scriptableOnly.value || t.scriptable) &&
                (q.isEmpty() ||
                    t.name.lowercase().contains(q) ||
                    t.desc.lowercase().contains(q) ||
                    t.id.lowercase().contains(q))
        }
    }
}
