package br.edu.fatecpg.appestacionamento.view.adapters

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.appestacionamento.R
import br.edu.fatecpg.appestacionamento.databinding.ItemAdminSpaceBinding
import br.edu.fatecpg.appestacionamento.model.ParkingSpace
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

// Adapter para a lista de vagas (visão do Admin)
class AdminSpaceAdapter(
    private val onEditClicked: (ParkingSpace) -> Unit,
    private val onDeleteClicked: (ParkingSpace) -> Unit
) : RecyclerView.Adapter<AdminSpaceAdapter.AdminSpaceViewHolder>() {

    private val differ = AsyncListDiffer(this, AdminSpaceDiffCallback())

    fun submitList(list: List<ParkingSpace>) {
        differ.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminSpaceViewHolder {
        val binding =
            ItemAdminSpaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminSpaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminSpaceViewHolder, position: Int) {
        holder.bind(differ.currentList[position])
    }

    override fun getItemCount() = differ.currentList.size


    inner class AdminSpaceViewHolder(private val binding: ItemAdminSpaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(space: ParkingSpace) {
            val context = itemView.context
            val resources = context.resources

            // Verifica se o modo escuro está ativo
            val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val isDarkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES

            binding.tvSpaceNumber.text = space.spaceNumber
            val ptBr = Locale("pt", "BR")
            val currencyFormat = NumberFormat.getCurrencyInstance(ptBr)
            binding.tvRate.text = "${currencyFormat.format(space.hourlyRate)} / hora"

            if (space.isOccupied) {
                binding.tvStatus.text = "Ocupada"

                if (isDarkMode) {
                    // Cores (Ocupada - Modo Escuro)
                    binding.cardSpace.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.status_occupied_red_dark_bg)
                    )
                    binding.tvStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.status_occupied_red_dark_text)
                    )
                    binding.tvSpaceNumber.setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary_light)
                    )
                    binding.tvRate.setTextColor(
                        ContextCompat.getColor(context, R.color.text_secondary_light)
                    )
                    binding.tvExpiry.setTextColor(
                        ContextCompat.getColor(context, R.color.status_occupied_red_dark_text)
                    )
                } else {
                    // Cores (Ocupada - Modo Claro)
                    binding.cardSpace.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.status_occupied_red)
                    )
                    binding.tvStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary_dark)
                    )
                    binding.tvSpaceNumber.setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary_dark)
                    )
                    binding.tvRate.setTextColor(
                        ContextCompat.getColor(context, R.color.text_secondary_gray)
                    )
                    binding.tvExpiry.setTextColor(
                        ContextCompat.getColor(context, R.color.status_occupied_red) // Texto vermelho
                    )
                }

                // Mostra detalhes da expiração
                binding.tvExpiry.visibility = View.VISIBLE
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                binding.tvExpiry.text = "Expira: ${space.reservationExpiry?.let { dateFormat.format(it) } ?: "N/A"}"

            } else {
                binding.tvStatus.text = "Livre"

                if (isDarkMode) {
                    // Cores (Livre - Modo Escuro)
                    binding.cardSpace.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.status_free_green_dark_bg)
                    )
                    binding.tvSpaceNumber.setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary_light)
                    )
                    binding.tvStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.status_free_green_dark_text)
                    )
                    binding.tvRate.setTextColor(
                        ContextCompat.getColor(context, R.color.text_secondary_light)
                    )
                } else {
                    // Cores (Livre - Modo Claro)
                    binding.cardSpace.setCardBackgroundColor(
                        ContextCompat.getColor(context, R.color.status_free_green)
                    )
                    binding.tvSpaceNumber.setTextColor(
                        ContextCompat.getColor(context, R.color.text_primary_dark)
                    )
                    binding.tvStatus.setTextColor(
                        ContextCompat.getColor(context, R.color.green_accent)
                    )
                    binding.tvRate.setTextColor(
                        ContextCompat.getColor(context, R.color.text_secondary_gray)
                    )
                }
                binding.tvExpiry.visibility = View.GONE
            }

            binding.btnEdit.setOnClickListener { onEditClicked(space) }
            binding.btnDelete.setOnClickListener { onDeleteClicked(space) }
        }
    }
}

class AdminSpaceDiffCallback : DiffUtil.ItemCallback<ParkingSpace>() {
    override fun areItemsTheSame(oldItem: ParkingSpace, newItem: ParkingSpace): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ParkingSpace, newItem: ParkingSpace): Boolean {
        return oldItem == newItem
    }
}