package com.bozkurt.short3k

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import coil.load
import coil.transform.RoundedCornersTransformation

class GameAdapter(
    private var games: List<GameModel>,
    private val onItemClick: (GameModel) -> Unit,
    private val onDeleteClick: (GameModel) -> Unit
) : RecyclerView.Adapter<GameAdapter.GameViewHolder>() {

    private var filteredGames: List<GameModel> = games

    fun updateList(newList: List<GameModel>) {
        games = newList
        filter("")
    }

    fun filter(query: String) {
        filteredGames = if (query.isEmpty()) {
            games
        } else {
            games.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.code.contains(query, ignoreCase = true) 
            }
        }
        notifyDataSetChanged()
    }

    class GameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivGameIcon)
        val ivBackground: ImageView = view.findViewById(R.id.ivGameBackground)
        val ivShortcutCheck: ImageView = view.findViewById(R.id.ivShortcutCheck)
        val ivDeleteShortcut: ImageView = view.findViewById(R.id.ivDeleteShortcut)
        val tvName: TextView = view.findViewById(R.id.tvGameName)
        val tvCode: TextView = view.findViewById(R.id.tvGameCode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = filteredGames[position]
        holder.tvName.text = game.name
        
        if (game.version == "MISSING") {
            val statusText = "${holder.itemView.context.getString(R.string.shortcut_exists)} | ${holder.itemView.context.getString(R.string.game_missing)}"
            holder.tvCode.text = statusText
            holder.tvCode.setTextColor(Color.RED)
        } else {
            holder.tvCode.text = "${game.code} | v${game.version}"
            holder.tvCode.setTextColor(Color.parseColor("#CCCCCC"))
        }
        
        // Shortcut Status
        if (game.hasShortcut) {
            holder.ivShortcutCheck.visibility = View.VISIBLE
            holder.ivDeleteShortcut.visibility = View.VISIBLE
        } else {
            holder.ivShortcutCheck.visibility = View.GONE
            holder.ivDeleteShortcut.visibility = View.GONE
        }
        
        holder.ivDeleteShortcut.setOnClickListener {
            onDeleteClick(game)
        }
        
        // Load Icon
        if (game.iconUri != null) {
            holder.ivIcon.load(Uri.parse(game.iconUri)) {
                crossfade(true)
                placeholder(R.mipmap.ic_launcher)
            }
        } else {
            holder.ivIcon.setImageResource(R.mipmap.ic_launcher)
        }

        // Load Background Art
        if (game.backgroundUri != null) {
            holder.ivBackground.load(Uri.parse(game.backgroundUri)) {
                crossfade(true)
            }
        } else {
            // Default gradient background if pic0.png is missing
            holder.ivBackground.setImageResource(android.R.color.transparent)
        }

        holder.itemView.setOnClickListener {
            onItemClick(game)
        }
    }

    override fun getItemCount() = filteredGames.size
}
