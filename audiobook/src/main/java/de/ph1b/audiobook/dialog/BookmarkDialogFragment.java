package de.ph1b.audiobook.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;

import de.ph1b.audiobook.R;
import de.ph1b.audiobook.adapter.BookmarkAdapter;
import de.ph1b.audiobook.model.Book;
import de.ph1b.audiobook.model.Bookmark;
import de.ph1b.audiobook.model.DataBaseHelper;
import de.ph1b.audiobook.model.NaturalBookmarkComparator;
import de.ph1b.audiobook.service.ServiceController;
import de.ph1b.audiobook.uitools.DividerItemDecoration;
import de.ph1b.audiobook.utils.BaseApplication;
import de.ph1b.audiobook.utils.L;

/**
 * @author <a href="mailto:woitaschek@posteo.de">Paul Woitaschek</a>
 * @link {http://www.paul-woitaschek.de}
 * @see <a href="http://www.paul-woitaschek.de">http://www.paul-woitaschek.de</a>
 */
public class BookmarkDialogFragment extends DialogFragment {

    public static final String TAG = BookmarkDialogFragment.class.getSimpleName();

    private BookmarkAdapter adapter;
    private MaterialDialog dialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        //passing null is fine because of fragment
        @SuppressLint("InflateParams") View v = inflater.inflate(R.layout.dialog_bookmark, null);

        final DataBaseHelper db = DataBaseHelper.getInstance(getActivity());
        final ServiceController controller = new ServiceController(getActivity());
        final Book book = ((BaseApplication) getActivity().getApplication()).getCurrentBook();
        if (book == null) {
            throw new AssertionError("Cannot instantiate " + TAG + " without a current book");
        }

        BookmarkAdapter.OnOptionsMenuClickedListener listener = new BookmarkAdapter.OnOptionsMenuClickedListener() {
            @Override
            public void onOptionsMenuClicked(final int position, View v) {
                PopupMenu popup = new PopupMenu(getActivity(), v);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.bookmark_popup, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                        switch (item.getItemId()) {
                            case R.id.edit:
                                final Bookmark editBookmark = adapter.getItem(position);

                                new MaterialDialog.Builder(getActivity())
                                        .title(R.string.bookmark_edit_title)
                                        .inputType(InputType.TYPE_CLASS_TEXT)
                                        .input(getString(R.string.bookmark_edit_hint), editBookmark.getTitle(), false, new MaterialDialog.InputCallback() {
                                            @Override
                                            public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                                                editBookmark.setTitle(charSequence.toString());
                                                db.updateBook(book);
                                                adapter.notifyItemChanged(position);
                                            }
                                        })
                                        .positiveText(R.string.dialog_confirm)
                                        .show();
                                return true;
                            case R.id.delete:
                                final Bookmark deleteBookmark = adapter.getItem(position);

                                builder.title(R.string.bookmark_delete_title)
                                        .content(deleteBookmark.getTitle())
                                        .positiveText(R.string.remove)
                                        .negativeText(R.string.dialog_cancel)
                                        .callback(new MaterialDialog.ButtonCallback() {
                                            @Override
                                            public void onPositive(MaterialDialog dialog) {
                                                adapter.removeItem(position);
                                                db.updateBook(book);
                                            }
                                        })
                                        .show();
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
            }

            @Override
            public void onBookmarkClicked(int position) {
                Bookmark bookmark = adapter.getItem(position);
                controller.changeTime(bookmark.getTime(), bookmark.getMediaPath());
                dialog.cancel();
            }
        };

        final RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler);
        adapter = new BookmarkAdapter(book, listener);
        recyclerView.setAdapter(adapter);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity()));
        recyclerView.setLayoutManager(layoutManager);

        final ImageView addButton = (ImageView) v.findViewById(R.id.add);
        final EditText bookmarkTitle = (EditText) v.findViewById(R.id.edit1);
        bookmarkTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addButton.performClick(); //same as clicking on the +
                    return true;
                }
                return false;
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = bookmarkTitle.getText().toString();
                if (title == null || title.equals("")) {
                    title = book.getCurrentChapter().getName();
                }

                Bookmark bookmark = new Bookmark(book.getCurrentChapter().getPath(), title, book.getTime());
                L.v(TAG, "Added bookmark=" + bookmark);

                book.getBookmarks().add(bookmark);
                Collections.sort(book.getBookmarks(), new NaturalBookmarkComparator(book.getChapters()));
                db.updateBook(book);
                bookmarkTitle.setText("");
                Toast.makeText(getActivity(), R.string.bookmark_added, Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });

        dialog = new MaterialDialog.Builder(getActivity())
                .customView(v, false)
                .title(R.string.bookmark)
                .negativeText(R.string.dialog_cancel)
                .build();

        return dialog;
    }
}