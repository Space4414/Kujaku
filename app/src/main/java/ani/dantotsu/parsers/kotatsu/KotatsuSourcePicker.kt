package ani.dantotsu.parsers.kotatsu

import android.app.Activity
import android.text.InputType
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import ani.dantotsu.R

/** Searchable multi-select picker for the Kotatsu manga parser catalogue. */
object KotatsuSourcePicker {
    fun show(activity: Activity, onSaved: () -> Unit) {
        val selected = KotatsuSources.enabledNames().toMutableSet()
        var shown = KotatsuSources.all

        val dialog = AlertDialog.Builder(activity)
        val themed = dialog.context
        val search = EditText(themed).apply {
            hint = activity.getString(R.string.kotatsu_search_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            setSingleLine()
        }
        val listView = ListView(themed).apply {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            val color = TypedValue()
            if (themed.theme.resolveAttribute(android.R.attr.colorBackground, color, true)) {
                setBackgroundColor(color.data)
            }
        }

        fun render() {
            val query = search.text.toString().trim().lowercase()
            shown = KotatsuSources.all.filter { source ->
                query.isEmpty() ||
                    source.title.lowercase().contains(query) ||
                    source.locale.lowercase() == query
            }
            listView.adapter = ArrayAdapter(
                themed,
                android.R.layout.simple_list_item_multiple_choice,
                shown.map { "${it.title}  (${it.locale.ifBlank { "multi" }})" },
            )
            shown.forEachIndexed { index, source ->
                listView.setItemChecked(index, source.name in selected)
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val source = shown.getOrNull(position) ?: return@setOnItemClickListener
            if (listView.isItemChecked(position)) selected.add(source.name)
            else selected.remove(source.name)
        }
        search.doAfterTextChanged { render() }
        render()

        val padding = (16 * activity.resources.displayMetrics.density).toInt()
        val content = LinearLayout(themed).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
            addView(search, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
            addView(listView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.55).toInt(),
            ))
        }

        dialog
            .setTitle(R.string.kotatsu_sources)
            .setView(content)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val previous = KotatsuSources.enabledNames().filter { it in selected }
                KotatsuSources.setEnabledNames(previous + selected.filter { it !in previous })
                onSaved()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
