package com.example.jetpackdemo.fragment.home.viewpager

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
import com.example.jetpackdemo.R
import com.example.jetpackdemo.adapter.DailyQuestionAdapter
import com.example.jetpackdemo.adapter.FooterAdapter
import com.example.jetpackdemo.databinding.FragmentDailyQuestionBinding
import com.example.jetpackdemo.util.ExceptionHandler.exceptionHandler
import com.example.jetpackdemo.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val TAG = "DailyQuestionFragment"
@AndroidEntryPoint
class DailyQuestionFragment : Fragment() {

    private var _binding: FragmentDailyQuestionBinding? = null
    private val binding get() = _binding!!

    private val homePageViewModel: HomePageViewModel by activityViewModels()
    private val collectViewModel: CollectViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val autoScrollViewModel: AutoScrollViewModel by activityViewModels()


    private val dailyQuestionAdapter by lazy{
        DailyQuestionAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"inner onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG,"inner onCreateView")
        _binding = FragmentDailyQuestionBinding.inflate(inflater, container, false)

        dailyQuestionAdapter.setOnItemClickListener { link, title ->
            Log.d(TAG,"link = $link title = $title")
            val navController = findNavController()
            // ????????? WebFragment
            val bundle = bundleOf("link" to link, "title" to title)
            //????????????????????? fragment
            navController.navigate(R.id.webFragment,bundle)
        }
        dailyQuestionAdapter.setImageViewClickListener{ id,collect->
            // ???????????????????????? ??????????????? ??????????????????????????? ??????
            Log.d(TAG,"collect = $collect")
            if (collect){
                collectViewModel.unCollect(id)
            }else {
                collectViewModel.collect(id)
            }
        }
        // ???RecyclerView??????adapter
        binding.recyclerview.adapter = dailyQuestionAdapter
            .withLoadStateFooter(FooterAdapter(dailyQuestionAdapter::retry))

        binding.swipeLayout.setOnRefreshListener {
            // ?????? PagingDataAdapter
            dailyQuestionAdapter.refresh()
        }

        binding.recyclerview.setTouchEventListener{
            Log.d(TAG,"inner setTouchEventListener")
            autoScrollViewModel.touchRecyclerview(it)
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

    private fun subscribeUI(){
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ?????? PagingData
                homePageViewModel.dailyQuestionFlow
                    .catch {
                        Log.d(TAG,"Exception : ${it.message}")
                    }
                    .collectLatest {
                        Log.d(TAG,"inner collectLatest")
                        // paging ?????? submitData ?????? adapter
                        dailyQuestionAdapter.submitData(it)
                    }
            }
        }

        //??????paging??????????????????
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dailyQuestionAdapter.loadStateFlow.collectLatest {
                    Log.d(TAG, "inner collectLatest loadState = $it")
                    binding.swipeLayout.isRefreshing = it.refresh is LoadState.Loading
                }
            }
        }

        // ??????
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collectViewModel.collectAction.collect {
                    when (it) {
                        is CollectAction.Success -> {
                            Log.d(TAG,"CollectAction.Success")
                            dailyQuestionAdapter.refresh()
                        }
                        is CollectAction.Error -> {
                            dailyQuestionAdapter.refresh()
                        }

                    }
                }
            }
        }

        // ????????????
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collectViewModel.unCollectAction.collect {
                    when (it) {
                        is UnCollectAction.Success -> {
                            Log.d(TAG,"UnCollectAction.Success")
                            dailyQuestionAdapter.refresh()
                        }
                        is UnCollectAction.Error -> {
                            dailyQuestionAdapter.refresh()
                        }

                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch(exceptionHandler) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.dailyQuestionState.collect {
                    Log.d(TAG, "inner dailyQuestionState collect it = $it")
                    when (it) {
                        is StateUiAction.StateChanged-> {
                            Log.d(TAG,"inner StateChanged")
                            // ????????????
                            dailyQuestionAdapter.refresh()
                            Log.d(TAG, "after dailyQuestionAdapter.refresh")
                            userViewModel.resetDailyQuestionState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = DailyQuestionFragment()
    }
}