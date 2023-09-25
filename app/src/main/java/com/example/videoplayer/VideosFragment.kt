package com.example.videoplayer

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.databinding.FragmentVideosBinding


class VideosFragment : Fragment() {


    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_videos, container, false)
        val binding = FragmentVideosBinding.bind(view)
        binding.videoRv.setHasFixedSize(true)
        binding.videoRv.setItemViewCacheSize(10)
        binding.videoRv.layoutManager = LinearLayoutManager(requireContext())
        binding.videoRv.adapter = VideoAdapter(requireContext(),MainActivity.videoList)

        binding.totalVideos.text = "Total Videos: ${MainActivity.videoList.size}"
        return view
    }


}