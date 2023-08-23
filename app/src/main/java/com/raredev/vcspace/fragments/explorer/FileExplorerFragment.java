package com.raredev.vcspace.fragments.explorer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.blankj.utilcode.util.ClipboardUtils;
import com.raredev.vcspace.activity.EditorActivity;
import com.raredev.vcspace.adapters.FileAdapter;
import com.raredev.vcspace.databinding.FragmentFileManagerBinding;
import com.raredev.vcspace.events.OnFileRenamedEvent;
import com.raredev.vcspace.fragments.explorer.git.CloneRepository;
import com.raredev.vcspace.models.FileModel;
import com.raredev.vcspace.res.R;
import com.raredev.vcspace.task.TaskExecutor;
import com.raredev.vcspace.ui.PathListView;
import com.raredev.vcspace.util.DialogUtils;
import com.raredev.vcspace.util.FileUtil;
import com.raredev.vcspace.util.ILogger;
import com.raredev.vcspace.util.PreferencesUtils;
import com.raredev.vcspace.viewmodel.FileListViewModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.greenrobot.eventbus.EventBus;

@SuppressWarnings("deprecation")
public class FileExplorerFragment extends Fragment {
/*  private static final String LOG = "FileManagerFragment";
  private FragmentFileManagerBinding binding;

  public FileListViewModel viewModel;

  private FileAdapter mFilesAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = new ViewModelProvider((ViewModelStoreOwner) this).get(FileListViewModel.class);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = FragmentFileManagerBinding.inflate(inflater, container, false);

    TooltipCompat.setTooltipText(binding.gitTools, getString(R.string.git));
    TooltipCompat.setTooltipText(binding.topbarMenu, getString(R.string.folder));
    binding.gitTools.setOnClickListener(
        v -> {
          PopupMenu pm = new PopupMenu(requireContext(), v);
          pm.getMenu().add(R.string.clone_repo);
          pm.setOnMenuItemClickListener(
              item -> {
                if (item.getTitle().equals(getString(R.string.clone_repo))) {
                  CloneRepository cloneRepo = new CloneRepository(requireActivity());
                  cloneRepo.setDirectory(viewModel.getCurrentDir().toFile());
                  cloneRepo.cloneRepository();
                  cloneRepo.setListener(
                      new CloneRepository.CloneListener() {

                        @Override
                        public void onCloneSuccess(File output) {
                          viewModel.setCurrentDir(FileModel.fileToFileModel(output));
                          ILogger.info(LOG, "Cloned to: " + output.toString());
                        }

                        @Override
                        public void onCloneFailed(String message) {
                          DialogUtils.newErrorDialog(requireActivity(), "Clone failed", message);
                          ILogger.error(LOG, "Clone failed: " + message);
                        }
                      });
                }
                return true;
              });
          pm.show();
        });

    binding.topbarMenu.setOnClickListener(
        v -> {
          PopupMenu pm = new PopupMenu(requireContext(), v);
          if (pm.getMenu() instanceof MenuBuilder) {
            ((MenuBuilder) pm.getMenu()).setOptionalIconsVisible(true);
          }
          pm.getMenu().add(R.string.refresh).setIcon(R.drawable.ic_refresh);
          pm.getMenu().add(R.string.new_file_title).setIcon(R.drawable.file_plus_outline);
          pm.getMenu().add(R.string.new_folder_title).setIcon(R.drawable.folder_plus_outline);
          pm.setOnMenuItemClickListener(
              item -> {
                if (item.getTitle().equals(getString(R.string.refresh))) {
                  refreshFiles();
                } else if (item.getTitle().equals(getString(R.string.new_file_title))) {
                  FileManagerDialogs.createFile(
                      requireContext(),
                      viewModel.getCurrentDir().toFile(),
                      (newFile) -> refreshFiles());
                } else if (item.getTitle().equals(getString(R.string.new_folder_title))) {
                  FileManagerDialogs.createFolder(
                      requireContext(),
                      viewModel.getCurrentDir().toFile(),
                      (newFolder) -> refreshFiles());
                }
                return true;
              });
          pm.show();
        });

    binding.pathList.setType(PathListView.TYPE_FOLDER_PATH);
    binding.pathList.setFileViewModel(viewModel);
    setupRecyclerView();

    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    viewModel
        .getFilesLiveData()
        .observe(
            getViewLifecycleOwner(),
            (files) -> {
              binding.container.setDisplayedChild(files.isEmpty() ? 2 : 0);
            });
    viewModel
        .getCurrentDirLiveData()
        .observe(
            getViewLifecycleOwner(),
            (dir) -> {
              listArchives(dir);
              binding.pathList.setPath(dir.getPath());
            });

    mFilesAdapter.setEditorViewModel(((EditorActivity) requireActivity()).viewModel);
  }

  @Override
  public void onFileClick(FileModel file, View v) {
    if (!file.isFile()) {
      viewModel.setCurrentDir(file);
    } else {
      if (FileUtil.isValidTextFile(file.getName())) {
        ((EditorActivity) requireActivity()).openFile(file);
      }
      if (file.getName().endsWith(".apk")) {
        FileManagerDialogs.showApkInfoDialog(requireContext(), file.toFile());
        // ApkInstaller.installApplication(getContext(), file.toFile());
      }
    }
  }

  @Override
  public void onFileLongClick(FileModel file, View v) {}

  @Override
  public void onFileMenuClick(FileModel file, View v) {
    PopupMenu pm = new PopupMenu(requireActivity(), v);
    if (pm.getMenu() instanceof MenuBuilder) {
      ((MenuBuilder) pm.getMenu()).setOptionalIconsVisible(true);
    }
    pm.getMenu().add(R.string.copy_path).setIcon(R.drawable.content_copy);
    pm.getMenu().add(R.string.rename).setIcon(R.drawable.file_rename);
    pm.getMenu().add(R.string.delete).setIcon(R.drawable.delete_outline);
    pm.setOnMenuItemClickListener(
        item -> {
          if (item.getTitle() == getString(R.string.copy_path)) {
            ClipboardUtils.copyText(file.getPath());
          } else if (item.getTitle() == getString(R.string.rename)) {
            FileManagerDialogs.renameFile(
                requireContext(),
                file.toFile(),
                (oldFile, newFile) -> {
                  EventBus.getDefault().post(new OnFileRenamedEvent(oldFile, newFile));
                  refreshFiles();
                });
          } else if (item.getTitle() == getString(R.string.delete)) {
            FileManagerDialogs.deleteFile(
                requireContext(), file.toFile(), (deletedFile) -> refreshFiles());
          }
          return true;
        });
    pm.show();
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshFiles();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  private void setupRecyclerView() {
    mFilesAdapter = new FileAdapter(viewModel);

    mFilesAdapter.setFileListener(this);

    binding.rvFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
    binding.rvFiles.setAdapter(mFilesAdapter);
  }

  public void onBackPressed() {
    viewModel.setCurrentDir(
        FileModel.fileToFileModel(viewModel.getCurrentDirFile().getParentFile()));
  }

  public void refreshFiles() {
    listArchives(viewModel.getCurrentDir());
  }

  public void listArchives(FileModel path) {
    binding.container.setDisplayedChild(1);
    TaskExecutor.executeAsyncProvideError(
        () -> {
          List<FileModel> mFiles = new ArrayList<>();
          path.listFiles(
              result -> {
                if (result.length == 0) {
                  return;
                }
                Arrays.sort(result, FILE_FIRST_ORDER);
                for (FileModel file : result) {
                  if (file.getName().startsWith(".") && !PreferencesUtils.showHiddenFiles()) {
                    continue;
                  }
                  mFiles.add(file);
                }
              });
          return mFiles;
        },
        (result, error) -> {
          binding.container.setDisplayedChild(0);
          if (result == null || error != null) {
            return;
          }
          viewModel.setFiles(result);
          mFilesAdapter.refreshFiles();
        });
  }

  public FileListViewModel getViewModel() {
    return viewModel;
  }

  public static final Comparator<FileModel> FILE_FIRST_ORDER =
      (file1, file2) -> {
        if (file1.isFile() && !file2.isFile()) {
          return 1;
        } else if (file2.isFile() && !file1.isFile()) {
          return -1;
        } else {
          return String.CASE_INSENSITIVE_ORDER.compare(file1.getName(), file2.getName());
        }
      };*/
}
