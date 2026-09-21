package salve.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import salve.presentation.viewmodel.DownloadUiState
import salve.presentation.viewmodel.ModelDownloadViewModel
import com.salve.app.databinding.FragmentDownloadOverlayBinding

class DownloadOverlayFragment : Fragment() {
    private var _binding: FragmentDownloadOverlayBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ModelDownloadViewModel by activityViewModels()
    private var hiddenState: Class<*>? = null
    private val hideBanner = Runnable { _binding?.overlayRoot?.visibility = View.GONE }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDownloadOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.overlayCloseButton.setOnClickListener {
            hiddenState = viewModel.uiState.value?.javaClass
            binding.overlayRoot.visibility = View.GONE
        }
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.overlayRoot.removeCallbacks(hideBanner)
            if (state is DownloadUiState.Idle || state.javaClass == hiddenState) {
                binding.overlayRoot.visibility = View.GONE
                return@observe
            }
            binding.overlayRoot.visibility = View.VISIBLE
            binding.overlayCloseButton.visibility = View.VISIBLE
            binding.overlayActionButton.visibility = View.GONE
            binding.overlayStatus.text = "Gemma 4 E2B · descarga externa"
            when (state) {
                is DownloadUiState.Idle -> Unit
                is DownloadUiState.Running -> {
                    binding.overlayTitle.text = "Preparando el modelo local"
                    binding.overlayProgress.progress = state.percent
                    binding.overlayProgress.isIndeterminate = state.percent == 0 || state.percent == 99
                    binding.overlayMessage.text = state.message ?: ""
                    binding.overlayActionButton.visibility = View.VISIBLE
                    binding.overlayActionButton.text = "Pausar"
                    binding.overlayActionButton.setOnClickListener { viewModel.pauseDownload() }
                }
                is DownloadUiState.Success -> {
                    binding.overlayTitle.text = "Modelo local activo"
                    binding.overlayProgress.isIndeterminate = false
                    binding.overlayProgress.progress = 100
                    binding.overlayMessage.text = state.message ?: "Inferencia comprobada"
                    binding.overlayRoot.postDelayed(hideBanner, 5000L)
                }
                is DownloadUiState.Error -> {
                    binding.overlayTitle.text = "Modelo pendiente de preparar"
                    binding.overlayProgress.isIndeterminate = false
                    binding.overlayProgress.progress = 0
                    binding.overlayMessage.text = state.message
                    binding.overlayActionButton.visibility = View.VISIBLE
                    binding.overlayActionButton.text = "Reanudar por Wi-Fi"
                    binding.overlayActionButton.setOnClickListener { hiddenState = null; viewModel.startDownload() }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.overlayRoot.removeCallbacks(hideBanner)
        super.onDestroyView()
        _binding = null
    }
}
