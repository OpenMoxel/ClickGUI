/**
 * - 开发者信息
 * - QQ：3969503151
 * - QQ邮箱：3969503151@qq.com
 * - 谷歌邮箱：atlasca3@gmail.com
 */
package com.pianai.xel;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/** Owns hidden IME input and search-query editing for the Canvas search field. */
final class ClickGuiSearchController {

    interface Callback {
        void onSearchVisualStateChanged();
    }

    private final Context context;
    private final ClickGuiState state;
    private final Callback callback;

    ClickGuiSearchController(Context context, ClickGuiState state, Callback callback) {
        this.context = context;
        this.state = state;
        this.callback = callback;
    }

    InputConnection createInputConnection(View view, EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        return new BaseInputConnection(view, false) {
            @Override
            public Editable getEditable() {
                return new SpannableStringBuilder(state.searchQuery);
            }

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                ClickGuiSearchController.this.commitText(text);
                return true;
            }

            @Override
            public boolean setComposingText(CharSequence text, int newCursorPosition) {
                ClickGuiSearchController.this.setComposingText(text);
                return true;
            }

            @Override
            public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                deleteCharacters(Math.max(1, beforeLength));
                return true;
            }

            @Override
            public boolean sendKeyEvent(KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP
                        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    deleteCharacters(1);
                    return true;
                }
                return super.sendKeyEvent(event);
            }

            @Override
            public boolean performEditorAction(int actionCode) {
                clearFocus(view);
                return true;
            }
        };
    }

    boolean onKeyUp(int keyCode) {
        if (state.searchFocused && keyCode == KeyEvent.KEYCODE_DEL) {
            deleteCharacters(1);
            return true;
        }
        return false;
    }

    void focus(View view) {
        state.searchFocused = true;
        view.requestFocus();
        view.postDelayed(() -> {
            InputMethodManager inputMethodManager = (InputMethodManager) context
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 120L);
        callback.onSearchVisualStateChanged();
    }

    void clearFocus(View view) {
        if (!state.searchFocused) {
            return;
        }
        state.searchFocused = false;
        InputMethodManager inputMethodManager = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        callback.onSearchVisualStateChanged();
    }

    private void commitText(CharSequence text) {
        if (text == null) {
            return;
        }
        String addition = text.toString().replace("\n", "").replace("\r", "");
        removeComposingText();
        if (!addition.isEmpty()) {
            state.searchQuery += addition;
        }
        state.listScrollY = 0f;
        callback.onSearchVisualStateChanged();
    }

    private void setComposingText(CharSequence text) {
        String composition = text == null ? ""
                : text.toString().replace("\n", "").replace("\r", "");
        removeComposingText();
        if (!composition.isEmpty()) {
            state.searchQuery += composition;
            state.composingSearchText = composition;
        }
        state.listScrollY = 0f;
        callback.onSearchVisualStateChanged();
    }

    private void removeComposingText() {
        if (!state.composingSearchText.isEmpty()
                && state.searchQuery.endsWith(state.composingSearchText)) {
            state.searchQuery = state.searchQuery.substring(0,
                    state.searchQuery.length() - state.composingSearchText.length());
        }
        state.composingSearchText = "";
    }

    private void deleteCharacters(int count) {
        removeComposingText();
        if (!state.searchQuery.isEmpty()) {
            int end = Math.max(0, state.searchQuery.length() - count);
            state.searchQuery = state.searchQuery.substring(0, end);
            state.listScrollY = 0f;
            callback.onSearchVisualStateChanged();
        }
    }
}
