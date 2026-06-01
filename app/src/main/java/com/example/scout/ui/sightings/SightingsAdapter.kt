package com.example.scout.ui.sightings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.scout.R
import com.example.scout.data.db.entities.SightingEntity
import com.example.scout.databinding.ItemSightingRowBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SightingsAdapter(
    private val onItemClick: (SightingEntity) -> Unit
) : ListAdapter<SightingEntity, SightingsAdapter.ViewHolder>(DiffCallback) {

    private val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemSightingRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SightingEntity) {
            binding.tvName.text = item.speciesName
            binding.tvDate.text = fmt.format(Date(item.loggedAt))
            binding.tvLocation.text = item.locationName ?: ""
            val notes = item.notes
            binding.tvNotes.text = notes
            binding.tvNotes.visibility = if (!notes.isNullOrBlank()) android.view.View.VISIBLE else android.view.View.GONE
            binding.tvCategory.text = item.category
            binding.ivPhoto.load(item.photoPath) {
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_placeholder)
            }
            binding.root.setOnClickListener {
                if (item.taxonId != null) onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSightingRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object DiffCallback : DiffUtil.ItemCallback<SightingEntity>() {
        override fun areItemsTheSame(old: SightingEntity, new: SightingEntity) = old.id == new.id
        override fun areContentsTheSame(old: SightingEntity, new: SightingEntity) = old == new
    }
}