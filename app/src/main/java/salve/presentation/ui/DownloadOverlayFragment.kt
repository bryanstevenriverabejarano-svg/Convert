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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadOverlayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DownloadUiState.Idle -> {
                    binding.overlayRoot.visibility = View.GONE
                }
                is DownloadUiState.Running -> {
                    binding.overlayRoot.visibility = View.VISIBLE
                    binding.overlayTitle.text = "Descargando modelos…"
                    binding.overlayProgress.progress = state.percent
                    binding.overlayMessage.text = state.message ?: ""
                    binding.overlayStatus.text = state.modelId ?: ""
                }
                is DownloadUiState.Success -> {
                    binding.overlayRoot.visibility = View.VISIBLE
                    binding.overlayTitle.text = "Modelos listos"
                    binding.overlayProgress.progress = 100
                    binding.overlayMessage.text = state.message ?: "Descarga completada"
                    binding.overlayStatus.text = ""
                }
                is DownloadUiState.Error -> {
                    binding.overlayRoot.visibility = View.VISIBLE
                    binding.overlayTitle.text = "Error descargando modelos"
                    binding.overlayProgress.progress = 0
                    binding.overlayMessage.text = state.message
                    binding.overlayStatus.text = ""
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
