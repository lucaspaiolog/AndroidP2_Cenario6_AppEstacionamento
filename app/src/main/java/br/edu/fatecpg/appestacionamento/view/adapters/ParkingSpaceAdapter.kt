package br.edu.fatecpg.appestacionamento.view.adapters

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import br.edu.fatecpg.appestacionamento.R
import br.edu.fatecpg.appestacionamento.databinding.ItemParkingSpaceBinding
import br.edu.fatecpg.appestacionamento.model.ParkingSpace
import java.text.NumberFormat
import java.util.Locale

// Adapter para a lista de vagas (visão do Motorista)
class ParkingSpaceAdapter(
    private val onSpaceClicked: (ParkingSpace) -> Unit
) : ListAdapter<ParkingSpace, ParkingSpaceAdapter.SpaceViewHolder>(SpaceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpaceViewHolder {
        val binding = ItemParkingSpaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SpaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SpaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SpaceViewHolder(private val binding: ItemParkingSpaceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(space: ParkingSpace) {
            binding.tvSpaceNumber.text = space.spaceNumber

            // Formata o valor da tarifa para Real (BRL)
            val ptBr = Locale("pt", "BR")
            val currencyFormat = NumberFormat.getCurrencyInstance(ptBr)
            binding.tvRate.text = "${currencyFormat.format(space.hourlyRate)} / hora"

            // Pega o contexto do item
            val context = itemView.context

            // Verifica se o sistema está em modo escuro
            val isNightMode = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            if (space.isOccupied) {
                // LÓGICA PARA VAGA OCUPADA
                binding.tvStatus.text = "Ocupada"
                binding.cardSpace.isClickable = false // Não pode clicar

                // Define as cores (escuro ou claro) para vaga OCUPADA
                val cardColor = if (isNightMode) R.color.status_occupied_red_dark_bg else R.color.status_occupied_red_bg
                val textColor = if (isNightMode) R.color.status_occupied_red_dark_text else R.color.status_occupied_red_text

                binding.cardSpace.setCardBackgroundColor(ContextCompat.getColor(context, cardColor))
                binding.tvSpaceNumber.setTextColor(ContextCompat.getColor(context, textColor))
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, textColor))
                binding.tvRate.setTextColor(ContextCompat.getColor(context, textColor))

            } else {
                // LÓGICA PARA VAGA LIVRE
                binding.tvStatus.text = "Livre"
                binding.cardSpace.isClickable = true // Pode clicar

                // Define as cores (escuro ou claro) para vaga LIVRE
                val cardColor = if (isNightMode) R.color.status_free_green_dark_bg else R.color.status_free_green_bg
                val textColor = if (isNightMode) R.color.status_free_green_dark_text else R.color.status_free_green_text

                binding.cardSpace.setCardBackgroundColor(ContextCompat.getColor(context, cardColor))
                binding.tvSpaceNumber.setTextColor(ContextCompat.getColor(context, textColor))
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, textColor))
                binding.tvRate.setTextColor(ContextCompat.getColor(context, textColor))

                // Define a ação de clique
                binding.cardSpace.setOnClickListener {
                    onSpaceClicked(space)
                }
            }
        }
    }
}

// Classe DiffUtil para otimizar o RecyclerView
class SpaceDiffCallback : DiffUtil.ItemCallback<ParkingSpace>() {
    override fun areItemsTheSame(oldItem: ParkingSpace, newItem: ParkingSpace): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ParkingSpace, newItem: ParkingSpace): Boolean {
        return oldItem == newItem
    }
}