package com.example.jetpackdemo.fragment.user

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.jetpackdemo.R
import com.example.jetpackdemo.adapter.CollectionAdapter
import com.example.jetpackdemo.adapter.FooterAdapter
import com.example.jetpackdemo.databinding.FragmentCollectionBinding
import com.example.jetpackdemo.util.ExceptionHandler.exceptionHandler
import com.example.jetpackdemo.viewmodel.CollectViewModel
import com.example.jetpackdemo.viewmodel.UnCollectAction
import com.example.jetpackdemo.viewmodel.UserViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "CollectionFragment"
@AndroidEntryPoint
class CollectionFragment : Fragment() {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!

    private val collectViewModel: CollectViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()


    private val collectionAdapter by lazy {
        CollectionAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG,"inner onCreateView")
       _binding = FragmentCollectionBinding.inflate(inflater, container, false)

        // ??????toolbar
        binding.toolbar.title = "????????????"
        binding.toolbar.setNavigationOnClickListener { view ->
            findNavController().navigateUp()
        }

        binding.recyclerview.setHasFixedSize(true)
        (binding.recyclerview.itemAnimator as SimpleItemAnimator?)!!.supportsChangeAnimations = false

        collectionAdapter.setOnItemClickListener { link, title ->
            Log.d(TAG,"link = $link title = $title")
            val navController = findNavController()
            // ????????? WebFragment
            val bundle = bundleOf("link" to link, "title" to title)
            //????????????????????? fragment
            navController.navigate(R.id.webFragment,bundle)
        }
        collectionAdapter.setImageViewClickListener { id,originId->
            // ????????????
            Log.d(TAG,"id = $id originId = $originId")
            collectViewModel.unCollectByCollection(id,originId)

            // ??????????????????????????????
            userViewModel.stateChanged()
        }

        val concatAdapter = collectionAdapter.withLoadStateFooter(FooterAdapter(collectionAdapter::retry))
        // ???RecyclerView??????adapter
        binding.recyclerview.adapter = concatAdapter

        binding.swipeLayout.setOnRefreshListener {
            // ?????? PagingDataAdapter
            collectionAdapter.refresh()
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeUI()
    }

    override fun onDestroyView() {
        Log.d(TAG,"inner onDestroyView")
        super.onDestroyView()
        binding.recyclerview.adapter = null
        _binding = null
    }

    private fun subscribeUI() {
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ?????? PagingData
                collectViewModel.collectionFlow
                    .catch {
                        Log.d(TAG,"Exception : ${it.message}")
                    }
                    .collectLatest {
                        Log.d(TAG,"inner collectLatest")
                        // paging ?????? submitData ?????? adapter
                        collectionAdapter.submitData(it)
                    }
            }
        }
        //??????paging??????????????????
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collectionAdapter.loadStateFlow.collectLatest {
                    Log.d(TAG, "inner collectLatest loadState = $it")
                    binding.swipeLayout.isRefreshing = it.refresh is LoadState.Loading
                }
            }
        }

        // ????????????
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collectViewModel.unCollectByCollectionAction.collect {
                    when (it) {
                        is UnCollectAction.Success -> {
                            Log.d(TAG,"UnCollectAction.Success")
                            Snackbar.make(
                                binding.root,
                                it.message,
                                Snackbar.LENGTH_SHORT
                            ).show()
                            // ???????????????????????????
                            collectionAdapter.refresh()
                        }
                        is UnCollectAction.Error -> {
                            Snackbar.make(
                                binding.root,
                                it.message,
                                Snackbar.LENGTH_SHORT
                            ).show()
                            collectionAdapter.refresh()
                        }

                    }
                }
            }
        }


    }


}