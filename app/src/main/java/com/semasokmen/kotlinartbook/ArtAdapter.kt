package com.semasokmen.kotlinartbook

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.semasokmen.kotlinartbook.databinding.RecyclerRowBinding

class ArtAdapter(val artList: ArrayList<Art>) : RecyclerView.Adapter<ArtAdapter.ArtHolder>() {

    class ArtHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root) {

    }
//bağlama:
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ArtHolder(binding)
    }

    override fun getItemCount(): Int {
        return artList.size
    }
//bağlandıktan sonrası: (texti gösterme)
    override fun onBindViewHolder(holder: ArtHolder, position: Int) {
        holder.binding.recyclerViewTextView.text = artList.get(position).name
    //tıklanınca ne olacağı:
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context,ArtActivity::class.java)
            intent.putExtra("info","old")
            //secilen sanat eserinin idsi:
            intent.putExtra("id",artList.get(position).id)
            holder.itemView.context.startActivity(intent)
        }


    }
}