package com.fanfou.app.service;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.BaseAdapter;

import com.fanfou.app.App;
import com.fanfou.app.App.ApnType;
import com.fanfou.app.R;
import com.fanfou.app.api.Api;
import com.fanfou.app.api.ApiException;
import com.fanfou.app.api.DirectMessage;
import com.fanfou.app.api.Parser;
import com.fanfou.app.api.Status;
import com.fanfou.app.api.User;
import com.fanfou.app.db.Contents.BasicColumns;
import com.fanfou.app.db.Contents.DirectMessageInfo;
import com.fanfou.app.db.Contents.StatusInfo;
import com.fanfou.app.db.Contents.UserInfo;
import com.fanfou.app.db.FanFouProvider;
import com.fanfou.app.http.ResponseCode;
import com.fanfou.app.ui.ActionManager.ResultListener;
import com.fanfou.app.ui.UIManager.ResultHandler;
import com.fanfou.app.util.StringHelper;
import com.fanfou.app.util.Utils;

import static com.fanfou.app.service.Constants.*;

/**
 * @author mcxiaoke
 * @version 1.0 20110602
 * @version 2.0 20110714
 * @version 2.1 2011.10.10
 * @version 3.0 2011.10.20
 * @version 3.1 2011.10.21
 * @version 3.2 2011.10.24
 * @version 3.3 2011.10.28
 * @version 4.0 2011.11.04
 * @version 4.1 2011.11.07
 * @version 4.2 2011.11.10
 * @version 4.3 2011.11.11
 * @version 4.4 2011.11.17
 * @version 5.0 2011.11.18
 * @version 5.1 2011.11.21
 * @version 5.2 2011.11.22
 * @version 5.3 2011.12.13
 * @version 6.0 2011.12.16
 * @version 6.1 2011.12.19
 * 
 */
public class FanFouService extends WakefulIntentService{
	private static final String TAG = FanFouService.class.getSimpleName();

	private ResultReceiver receiver;
	private int type;

	public FanFouService() {
		super("FetchService");
	}

	public void log(String message) {
		Log.d(TAG, message);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent == null) {
			return;
		}
		receiver = intent.getParcelableExtra(EXTRA_RECEIVER);
		type = intent.getIntExtra(EXTRA_TYPE, -1);

		if (App.DEBUG) {
			log("onHandleIntent() type=" + type);
		}

		switch (type) {
		case TYPE_NONE:
			break;
		case TYPE_ACCOUNT_REGISTER:
			break;
		case TYPE_ACCOUNT_VERIFY_CREDENTIALS:
			break;
		case TYPE_ACCOUNT_RATE_LIMIT_STATUS:
			break;
		case TYPE_ACCOUNT_UPDATE_PROFILE:
			break;
		case TYPE_ACCOUNT_UPDATE_PROFILE_IMAGE:
			break;
		case TYPE_ACCOUNT_NOTIFICATION:
			break;
		case TYPE_STATUSES_HOME_TIMELINE:
		case TYPE_STATUSES_MENTIONS:
		case TYPE_STATUSES_USER_TIMELINE:
		case TYPE_STATUSES_CONTEXT_TIMELINE:
		case TYPE_STATUSES_PUBLIC_TIMELINE:
		case TYPE_FAVORITES_LIST:
			fetchTimeline(intent);
			break;
		case TYPE_STATUSES_SHOW:
			statusesShow(intent);
			break;
		case TYPE_STATUSES_UPDATE:
			break;
		case TYPE_STATUSES_DESTROY:
			statusesDestroy(intent);
			break;
		case TYPE_DIRECT_MESSAGES_INBOX:
			fetchDirectMessagesInbox(intent);
			break;
		case TYPE_DIRECT_MESSAGES_OUTBOX:
			fetchDirectMessagesOutbox(intent);
			break;
		case TYPE_DIRECT_MESSAGES_CONVERSTATION_LIST:
			fetchConversationList(intent);
			break;
		case TYPE_DIRECT_MESSAGES_CONVERSTATION:
			break;
		case TYPE_DIRECT_MESSAGES_CREATE:
			break;
		case TYPE_DIRECT_MESSAGES_DESTROY:
			directMessagesDelete(intent);
			break;
		case TYPE_USERS_SHOW:
			userShow(intent);
			break;
		case TYPE_USERS_FRIENDS:
		case TYPE_USERS_FOLLOWERS:
			fetchUsers(intent);
			break;
		case TYPE_FRIENDSHIPS_CREATE:
			friendshipsCreate(intent);
			break;
		case TYPE_FRIENDSHIPS_DESTROY:
			friendshipsDelete(intent);
			break;
		case TYPE_FRIENDSHIPS_EXISTS:
			friendshipsExists(intent);
			break;
		case TYPE_FRIENDSHIPS_SHOW:
			break;
		case TYPE_FRIENDSHIPS_REQUESTS:
			break;
		case TYPE_FRIENDSHIPS_DENY:
			break;
		case TYPE_FRIENDSHIPS_ACCEPT:
			break;
		case TYPE_BLOCKS:
			break;
		case TYPE_BLOCKS_IDS:
			break;
		case TYPE_BLOCKS_CREATE:
			blocksCreate(intent);
			break;
		case TYPE_BLOCKS_DESTROY:
			blocksDelete(intent);
			break;
		case TYPE_BLOCKS_EXISTS:
			break;
		case TYPE_FRIENDS_IDS:
			break;
		case TYPE_FOLLOWERS_IDS:
			break;
		case TYPE_FAVORITES_CREATE:
			favoritesCreate(intent);
			break;
		case TYPE_FAVORITES_DESTROY:
			favoritesDelete(intent);
			break;
		case TYPE_PHOTOS_USER_TIMELINE:
			break;
		case TYPE_PHOTOS_UPLOAD:
			break;
		case TYPE_SEARCH_PUBLIC_TIMELINE:
			break;
		case TYPE_SEARCH_USER_TIMELINE:
			break;
		case TYPE_SEARCH_USERS:
			break;
		case TYPE_SAVED_SEARCHES_LIST:
			break;
		case TYPE_SAVED_SEARCHES_SHOW:
			break;
		case TYPE_SAVED_SEARCHES_CREATE:
			break;
		case TYPE_SAVED_SEARCHES_DESTROY:
			break;
		case TYPE_TRENDS_LIST:
		default:
			break;
		}

	}

	public static void doMessageDelete(final Activity activity,
			final String id, final ResultListener li, final boolean finish) {
		if (StringHelper.isEmpty(id)) {
			if (App.DEBUG) {
				Log.d(TAG, "doMessageDelete: status id is null.");
			}
			throw new NullPointerException("directmessageid cannot be null.");
		}
		ResultReceiver receiver = new ResultReceiver(new Handler(
				activity.getMainLooper())) {

			@Override
			protected void onReceiveResult(int resultCode, Bundle resultData) {
				switch (resultCode) {
				case Constants.RESULT_SUCCESS:
					Utils.notify(activity.getApplicationContext(), "删除成功");
					onSuccess(li, Constants.TYPE_DIRECT_MESSAGES_DESTROY,
							"删除成功");
					if (finish && activity != null) {
						activity.finish();
					}
					break;
				case Constants.RESULT_ERROR:
					String msg = resultData.getString(Constants.EXTRA_ERROR);
					Utils.notify(activity.getApplicationContext(), msg);
					onFailed(li, Constants.TYPE_DIRECT_MESSAGES_DESTROY, "删除失败");
					break;
				default:
					break;
				}
			}
		};
		FanFouService.doDirectMessagesDelete(activity, id, receiver);
	}

	public static void doDirectMessagesDelete(Context context, String id,
			final ResultReceiver receiver) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_DIRECT_MESSAGES_DESTROY);
		intent.putExtra(EXTRA_ID, id);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		context.startService(intent);
	}

	private void directMessagesDelete(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		String where = BasicColumns.ID + "=?";
		String[] whereArgs = new String[] { id };
		try {
			// 删除消息
			// 404 说明消息不存在
			// 403 说明不是你的消息，无权限删除
			DirectMessage dm = App.getApi().directMessagesDelete(id, MODE);
			if (dm == null || dm.isNull()) {
				sendSuccessMessage();
			} else {
				ContentResolver cr = getContentResolver();
				int result = cr.delete(DirectMessageInfo.CONTENT_URI, where,
						whereArgs);
				sendParcelableMessage(dm);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	private void blocksCreate(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		String where = BasicColumns.ID + "=?";
		String[] whereArgs = new String[] { id };
		try {
			User u = App.getApi().blocksCreate(id, MODE);
			if (u == null || u.isNull()) {
				sendSuccessMessage();
			} else {
				getContentResolver().delete(UserInfo.CONTENT_URI, where,
						whereArgs);
				sendParcelableMessage(u);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	private void blocksDelete(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		try {
			User u = App.getApi().blocksDelete(id, MODE);
			if (u == null || u.isNull()) {
				sendSuccessMessage();
			} else {
				sendParcelableMessage(u);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	public static void doFollow(Context context, final User user,
			final ResultReceiver receiver) {
		if (user.following) {
			doUnFollow(context, user.id, receiver);
		} else {
			doFollow(context, user.id, receiver);
		}
	}

	public static void doFollow(Context context, String userId,
			final ResultReceiver receiver) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_FRIENDSHIPS_CREATE);
		intent.putExtra(EXTRA_ID, userId);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		context.startService(intent);

	}

	public static void doUnFollow(Context context, String userId,
			final ResultReceiver receiver) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_FRIENDSHIPS_DESTROY);
		intent.putExtra(EXTRA_ID, userId);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		context.startService(intent);

	}

	private void friendshipsCreate(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		try {
			User u = App.getApi().friendshipsCreate(id, MODE);
			if (u == null || u.isNull()) {
				sendSuccessMessage();
			} else {
				u.type = Constants.TYPE_USERS_FRIENDS;
				getContentResolver().insert(UserInfo.CONTENT_URI,
						u.toContentValues());
				sendParcelableMessage(u);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	private void friendshipsDelete(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		try {
			User u = App.getApi().friendshipsDelete(id, MODE);
			if (u == null || u.isNull()) {
				sendSuccessMessage();
			} else {
				u.type = TYPE_NONE;
				ContentResolver cr = getContentResolver();
				cr.delete(UserInfo.CONTENT_URI, BasicColumns.ID + "=?",
						new String[] { id });
				sendParcelableMessage(u);
				// 取消关注后要清空该用户名下的消息
				cr.delete(StatusInfo.CONTENT_URI, StatusInfo.USER_ID + "=?",
						new String[] { id });
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	private void userShow(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		try {
			User u = App.getApi().userShow(id, MODE);
			if (u == null || u.isNull()) {
				sendSuccessMessage();
			} else {
				if (!FanFouProvider.updateUserInfo(this, u)) {
					FanFouProvider.insertUserInfo(this, u);
				}
				sendParcelableMessage(u);

			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	public static void doFavorite(final Activity activity, final Status status) {
		doFavorite(activity, status, null, false);
	}

	public static void doFavorite(final Activity activity, final Status status,
			boolean finish) {
		doFavorite(activity, status, null, finish);
	}

	public static void doFavorite(final Activity activity, final Status status,
			final ResultListener li) {
		doFavorite(activity, status, li, false);
	}

	public static void doFavorite(final Activity activity, final Status status,
			final ResultListener li, final boolean finish) {
		if (status == null || status.isNull()) {
			if (App.DEBUG) {
				Log.d(TAG, "doFavorite: status is null.");
			}
			throw new NullPointerException("status cannot be null.");
		}
		final int type = status.favorited ? Constants.TYPE_FAVORITES_DESTROY
				: Constants.TYPE_FAVORITES_CREATE;
		ResultReceiver receiver = new ResultReceiver(new Handler(
				activity.getMainLooper())) {

			@Override
			protected void onReceiveResult(int resultCode, Bundle resultData) {
				switch (resultCode) {
				case Constants.RESULT_SUCCESS:
					Status result = (Status) resultData
							.getParcelable(Constants.EXTRA_DATA);
					String text = result.favorited ? "收藏成功" : "取消收藏成功";
					Utils.notify(activity.getApplicationContext(), text);
					onSuccess(li, type, text);
					if (finish) {
						activity.finish();
					}
					break;
				case Constants.RESULT_ERROR:
					String msg = resultData.getString(Constants.EXTRA_ERROR);
					Utils.notify(activity.getApplicationContext(), msg);
					onFailed(li, type, "收藏失败");
					break;
				default:
					break;
				}
			}
		};
		if (status.favorited) {
			FanFouService.doUnfavorite(activity, status.id, receiver);
		} else {
			FanFouService.doFavorite(activity, status.id, receiver);
		}
	}

	public static void doFavorite(final Activity activity, final Status s,
			final BaseAdapter adapter) {
		ResultHandler li = new ResultHandler() {
			@Override
			public void onActionSuccess(int type, String message) {
				if (type == Constants.TYPE_FAVORITES_CREATE) {
					s.favorited = true;
				} else {
					s.favorited = false;
				}
				adapter.notifyDataSetChanged();
			}
		};
		doFavorite(activity, s, li);
	}

	public static void doFavorite(final Activity activity, final Status s,
			final Cursor c) {
		ResultHandler li = new ResultHandler() {
			@Override
			public void onActionSuccess(int type, String message) {
				c.requery();
			}
		};
		doFavorite(activity, s, li);
	}

	public static void doFavorite(Context context, String id,
			final ResultReceiver receiver) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_FAVORITES_CREATE);
		intent.putExtra(EXTRA_ID, id);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		context.startService(intent);
	}

	private void favoritesCreate(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		String where = BasicColumns.ID + "=?";
		String[] whereArgs = new String[] { id };
		try {
			Status s = App.getApi().favoritesCreate(id, FORMAT, MODE);
			if (s == null || s.isNull()) {
				sendSuccessMessage();
			} else {
				ContentResolver cr = getContentResolver();
				ContentValues values = new ContentValues();
				values.put(StatusInfo.FAVORITED, true);
				int result = cr.update(StatusInfo.CONTENT_URI, values, where,
						whereArgs);
				FanFouProvider.updateUserInfo(this, s.user);
				sendParcelableMessage(s);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			if (e.statusCode == 404) {
				Uri uri = FanFouProvider.buildUriWithStatusId(id);
				getContentResolver().delete(uri, null, null);
			}
			sendErrorMessage(e);
		}
	}

	public static void doUnfavorite(Context context, String id,
			final ResultReceiver receiver) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_FAVORITES_DESTROY);
		intent.putExtra(EXTRA_ID, id);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		context.startService(intent);
	}

	private void favoritesDelete(Intent intent) {
		// 404 消息不存在
		// 404 没有通过用户验证
		String id = intent.getStringExtra(EXTRA_ID);
		String where = BasicColumns.ID + "=?";
		String[] whereArgs = new String[] { id };
		try {
			Status s = App.getApi().favoritesDelete(id, FORMAT, MODE);
			if (s == null || s.isNull()) {
				sendSuccessMessage();
			} else {
				ContentResolver cr = getContentResolver();
				ContentValues values = new ContentValues();
				values.put(StatusInfo.FAVORITED, false);
				int result = cr.update(StatusInfo.CONTENT_URI, values, where,
						whereArgs);
				FanFouProvider.updateUserInfo(this, s.user);
				sendParcelableMessage(s);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			if (e.statusCode == 404) {
				Uri uri = FanFouProvider.buildUriWithStatusId(id);
				getContentResolver().delete(uri, null, null);
			}
			sendErrorMessage(e);
		}
	}

	public static void doStatusDelete(final Activity activity, final String id) {
		doStatusDelete(activity, id, null);
	}

	public static void doStatusDelete(final Activity activity, final String id,
			final ResultListener li) {
		doStatusDelete(activity, id, li, false);
	}

	public static void doStatusDelete(final Activity activity, final String id,
			final boolean finish) {
		doStatusDelete(activity, id, null, finish);
	}

	public static void doStatusDelete(final Activity activity, final String id,
			final ResultListener li, final boolean finish) {
		if (StringHelper.isEmpty(id)) {
			if (App.DEBUG) {
				Log.d(TAG, "doStatusDelete: status id is null.");
			}
			throw new NullPointerException("statusid cannot be null.");
		}
		ResultReceiver receiver = new ResultReceiver(new Handler(
				activity.getMainLooper())) {

			@Override
			protected void onReceiveResult(int resultCode, Bundle resultData) {
				switch (resultCode) {
				case Constants.RESULT_SUCCESS:
					Utils.notify(activity.getApplicationContext(), "删除成功");
					onSuccess(li, Constants.TYPE_STATUSES_DESTROY, "删除成功");
					if (finish && activity != null) {
						activity.finish();
					}
					break;
				case Constants.RESULT_ERROR:
					String msg = resultData.getString(Constants.EXTRA_ERROR);
					Utils.notify(activity.getApplicationContext(), msg);
					onFailed(li, Constants.TYPE_STATUSES_DESTROY, "删除失败");
					break;
				default:
					break;
				}
			}
		};
		FanFouService.doStatusesDelete(activity, id, receiver);
	}

	public static void doStatusesDelete(Context context, String id,
			final ResultReceiver receiver) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_STATUSES_DESTROY);
		intent.putExtra(EXTRA_ID, id);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		context.startService(intent);
	}

	private void statusesDestroy(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		try {
			Status s = App.getApi().statusesDelete(id, FORMAT, MODE);
			if (s == null || s.isNull()) {
				sendSuccessMessage();
			} else {
				ContentResolver cr = getContentResolver();
				Uri uri = Uri.parse(StatusInfo.CONTENT_URI + "/id/" + id);
				int result = cr.delete(uri, null, null);
				sendParcelableMessage(s);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			if (e.statusCode == 404) {
				Uri uri = FanFouProvider.buildUriWithStatusId(id);
				getContentResolver().delete(uri, null, null);
			}
			sendErrorMessage(e);
		}
	}

	public static void doProfile(Context context, String userId,
			final ResultReceiver receiver) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_USERS_SHOW);
		intent.putExtra(EXTRA_ID, userId);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		context.startService(intent);
	}

	private void statusesShow(Intent intent) {
		String id = intent.getStringExtra(EXTRA_ID);
		try {
			Status s = App.getApi().statusesShow(id, FORMAT, MODE);
			if (s == null || s.isNull()) {
				sendSuccessMessage();
			} else {
				if (!FanFouProvider.updateUserInfo(this, s.user)) {
					FanFouProvider.insertUserInfo(this, s.user);
				}
				FanFouProvider.updateUserInfo(this, s.user);
				sendParcelableMessage(s);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			if (e.statusCode == 404) {
				Uri uri = FanFouProvider.buildUriWithStatusId(id);
				getContentResolver().delete(uri, null, null);
			}
			sendErrorMessage(e);
		}

	}

	public static void doFriendshipsExists(Context context, String userA,
			String userB, final ResultReceiver receiver) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_FRIENDSHIPS_EXISTS);
		intent.putExtra("user_a", userA);
		intent.putExtra("user_b", userB);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		context.startService(intent);
	}

	private void friendshipsExists(Intent intent) {
		String userA = intent.getStringExtra("user_a");
		String userB = intent.getStringExtra("user_b");
		Api api = App.getApi();
		boolean result = false;
		try {
			result = api.friendshipsExists(userA, userB);
		} catch (ApiException e) {
			if (App.DEBUG) {
				Log.e(TAG, "doDetectFriendships:" + e.getMessage());
			}
			sendErrorMessage(e);
		}
		Bundle data = new Bundle();
		data.putBoolean(EXTRA_BOOLEAN, result);
		sendSuccessMessage(data);
	}

	private void fetchUsers(Intent intent) {
		String ownerId = intent.getStringExtra(EXTRA_ID);
		int page = intent.getIntExtra(EXTRA_PAGE, 0);
		int count = intent.getIntExtra(EXTRA_COUNT, DEFAULT_USERS_COUNT);
		if (App.DEBUG)
			log("fetchFriendsOrFollowers ownerId=" + ownerId + " page=" + page);

		if (App.getApnType() == ApnType.WIFI) {
			count = MAX_USERS_COUNT;
		} else {
			count = DEFAULT_USERS_COUNT;
		}

		Api api = App.getApi();
		try {
			List<User> users = null;
			if (type == TYPE_USERS_FRIENDS) {
				users = api.usersFriends(ownerId, count, page, MODE);
			} else if (type == TYPE_USERS_FOLLOWERS) {
				users = api.usersFollowers(ownerId, count, page, MODE);
			}
			if (users != null && users.size() > 0) {

				int size = users.size();
				if (App.DEBUG) {
					log("fetchFriendsOrFollowers size=" + size);
				}
				ContentResolver cr = getContentResolver();
				if (page < 2 && ownerId != null) {
					String where = BasicColumns.OWNER_ID + " =? ";
					String[] whereArgs = new String[] { ownerId };
					int deletedNums = cr.delete(UserInfo.CONTENT_URI, where,
							whereArgs);
					if (App.DEBUG) {
						log("fetchFriendsOrFollowers delete old rows "
								+ deletedNums);
					}
				}
				int nums = cr.bulkInsert(UserInfo.CONTENT_URI,
						Parser.toContentValuesArray(users));
				if (App.DEBUG) {
					log("fetchFriendsOrFollowers refresh ,insert rows, num="
							+ nums);
				}
				sendIntMessage(nums);
			} else {
				sendIntMessage(0);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	private void fetchConversationList(Intent intent) {
		int count = intent.getIntExtra(EXTRA_COUNT,
				count = DEFAULT_TIMELINE_COUNT);
		if (App.getApnType() == ApnType.WIFI) {
			count = MAX_TIMELINE_COUNT;
		} else {
			count = DEFAULT_TIMELINE_COUNT;
		}
		boolean doGetMore = intent.getBooleanExtra(EXTRA_BOOLEAN, false);
		try {
			if (doGetMore) {
				sendIntMessage(fetchOldDirectMessages(count));
			} else {
				sendIntMessage(fetchNewDirectMessages(count));
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	public static void doFetchDirectMessagesConversationList(Context context,
			final ResultReceiver receiver, boolean doGetMore) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, TYPE_DIRECT_MESSAGES_CONVERSTATION_LIST);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		intent.putExtra(EXTRA_BOOLEAN, doGetMore);
		context.startService(intent);
	}

	private void fetchDirectMessagesInbox(Intent intent) {
		int count = intent.getIntExtra(EXTRA_COUNT,
				count = DEFAULT_TIMELINE_COUNT);
		if (App.getApnType() == ApnType.WIFI) {
			count = MAX_TIMELINE_COUNT;
		} else {
			count = DEFAULT_TIMELINE_COUNT;
		}
		boolean doGetMore = intent.getBooleanExtra(EXTRA_BOOLEAN, false);
		try {
			sendIntMessage(fetchDirectMessagesInbox(count, doGetMore));
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	private void fetchDirectMessagesOutbox(Intent intent) {
		int count = intent.getIntExtra(EXTRA_COUNT,
				count = DEFAULT_TIMELINE_COUNT);
		if (App.getApnType() == ApnType.WIFI) {
			count = MAX_TIMELINE_COUNT;
		} else {
			count = DEFAULT_TIMELINE_COUNT;
		}
		boolean doGetMore = intent.getBooleanExtra(EXTRA_BOOLEAN, false);
		try {
			sendIntMessage(fetchDirectMessagesOutbox(count, doGetMore));
		} catch (ApiException e) {
			if (App.DEBUG) {
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	private int fetchDirectMessagesInbox(int count, boolean doGetMore)
			throws ApiException {
		Api api = App.getApi();
		Cursor ic = initMessagesCursor(false);
		List<DirectMessage> messages = null;
		if (doGetMore) {
			messages = api.directMessagesInbox(count, 0, null,
					Utils.getDmMaxId(ic), MODE);
		} else {
			messages = api.directMessagesInbox(count, 0,
					Utils.getDmSinceId(ic), null, MODE);
		}
		ic.close();
		if (messages != null && messages.size() > 0) {
			ContentResolver cr = getContentResolver();
			int size = messages.size();
			if (App.DEBUG) {
				log("fetchDirectMessagesInbox size()=" + size);
			}
			int nums = cr.bulkInsert(DirectMessageInfo.CONTENT_URI,
					Parser.toContentValuesArray(messages));
			return nums;
		} else {
			if (App.DEBUG) {
				log("fetchDirectMessagesInbox size()=0");
			}
		}
		return 0;
	}

	private int fetchDirectMessagesOutbox(int count, boolean doGetMore)
			throws ApiException {
		Api api = App.getApi();
		Cursor ic = initMessagesCursor(true);
		List<DirectMessage> messages = null;
		if (doGetMore) {
			messages = api.directMessagesOutbox(count, 0, null,
					Utils.getDmMaxId(ic), MODE);
		} else {
			messages = api.directMessagesOutbox(count, 0,
					Utils.getDmSinceId(ic), null, MODE);
		}
		ic.close();
		if (messages != null && messages.size() > 0) {
			ContentResolver cr = getContentResolver();
			int size = messages.size();
			if (App.DEBUG) {
				log("fetchDirectMessagesOutbox size()=" + size);
			}
			int nums = cr.bulkInsert(DirectMessageInfo.CONTENT_URI,
					Parser.toContentValuesArray(messages));
			return nums;
		} else {
			if (App.DEBUG) {
				log("fetchDirectMessagesOutbox size()=0");
			}
		}
		return 0;
	}

	private int fetchNewDirectMessages(int count) throws ApiException {
		Api api = App.getApi();
		Cursor ic = initMessagesCursor(false);
		Cursor oc = initMessagesCursor(true);
		try {
			String inboxSinceId = Utils.getDmSinceId(ic);
			String outboxSinceId = Utils.getDmSinceId(oc);
			List<DirectMessage> messages = new ArrayList<DirectMessage>();
			List<DirectMessage> in = api.directMessagesInbox(count, 0,
					inboxSinceId, null, MODE);
			if (in != null && in.size() > 0) {
				messages.addAll(in);
			}
			List<DirectMessage> out = api.directMessagesOutbox(count, 0,
					outboxSinceId, null, MODE);
			if (out != null && out.size() > 0) {
				messages.addAll(out);
			}
			if (messages != null && messages.size() > 0) {
				ContentResolver cr = getContentResolver();
				int size = messages.size();
				if (App.DEBUG) {
					log("fetchNewDirectMessages size()=" + size);
				}
				int nums = cr.bulkInsert(DirectMessageInfo.CONTENT_URI,
						Parser.toContentValuesArray(messages));
				return nums;
			} else {
				if (App.DEBUG) {
					log("fetchNewDirectMessages size()=0");
				}
			}
		} finally {
			oc.close();
			ic.close();
		}
		return 0;
	}

	private int fetchOldDirectMessages(int count) throws ApiException {
		Api api = App.getApi();
		Cursor ic = initMessagesCursor(false);
		Cursor oc = initMessagesCursor(true);
		try {
			String inboxMaxId = Utils.getDmMaxId(ic);
			String outboxMaxid = Utils.getDmMaxId(oc);
			List<DirectMessage> messages = new ArrayList<DirectMessage>();
			List<DirectMessage> in = api.directMessagesInbox(count, 0, null,
					inboxMaxId, MODE);
			if (in != null && in.size() > 0) {
				messages.addAll(in);
			}
			List<DirectMessage> out = api.directMessagesOutbox(count, 0, null,
					outboxMaxid, MODE);
			if (out != null && out.size() > 0) {
				messages.addAll(out);
			}
			if (messages != null && messages.size() > 0) {
				ContentResolver cr = getContentResolver();
				int size = messages.size();
				if (App.DEBUG) {
					log("doFetchMessagesMore size()=" + size);
				}
				int nums = cr.bulkInsert(DirectMessageInfo.CONTENT_URI,
						Parser.toContentValuesArray(messages));
				return nums;
			} else {
				if (App.DEBUG) {
					log("doFetchMessagesMore size()=0");
				}
			}
		} finally {
			oc.close();
			ic.close();
		}
		return 0;
	}

	private Cursor initMessagesCursor(final boolean outbox) {
		String where = BasicColumns.TYPE + " = ? ";
		String[] whereArgs = new String[] { String
				.valueOf(outbox ? TYPE_DIRECT_MESSAGES_INBOX
						: TYPE_DIRECT_MESSAGES_OUTBOX) };
		return getContentResolver().query(DirectMessageInfo.CONTENT_URI,
				DirectMessageInfo.COLUMNS, where, whereArgs, null);
	}

	private void fetchTimeline(Intent intent) {
		if (App.DEBUG) {
			Log.d(TAG, "fetchTimeline");
		}
		Api api = App.getApi();
		List<Status> statuses = null;

		int page = intent.getIntExtra(EXTRA_PAGE, 0);
		String id = intent.getStringExtra(EXTRA_ID);
		String sinceId = intent.getStringExtra(EXTRA_SINCE_ID);
		String maxId = intent.getStringExtra(EXTRA_MAX_ID);

		int count = intent.getIntExtra(EXTRA_COUNT, DEFAULT_TIMELINE_COUNT);
		if (App.getApnType() == ApnType.WIFI) {
			count = MAX_TIMELINE_COUNT;
		} else {
			count = DEFAULT_TIMELINE_COUNT;
		}
		try {
			switch (type) {
			case TYPE_STATUSES_HOME_TIMELINE:
				if (App.DEBUG)
					Log.d(TAG, "fetchTimeline TYPE_HOME");
				statuses = api.homeTimeline(count, page, sinceId, maxId,
						FORMAT, MODE);

				break;
			case TYPE_STATUSES_MENTIONS:
				if (App.DEBUG)
					Log.d(TAG, "fetchTimeline TYPE_MENTION");
				statuses = api.mentions(count, page, sinceId, maxId, FORMAT,
						MODE);
				break;
			case TYPE_STATUSES_PUBLIC_TIMELINE:
				count = DEFAULT_TIMELINE_COUNT;
				if (App.DEBUG)
					Log.d(TAG, "fetchTimeline TYPE_PUBLIC");
				statuses = api.pubicTimeline(count, FORMAT, MODE);
				break;
			case TYPE_FAVORITES_LIST:
				if (App.DEBUG)
					Log.d(TAG, "fetchTimeline TYPE_FAVORITES");
				statuses = api.favorites(count, page, id, FORMAT, MODE);
				break;
			case TYPE_STATUSES_USER_TIMELINE:
				if (App.DEBUG)
					Log.d(TAG, "fetchTimeline TYPE_USER");
				statuses = api.userTimeline(count, page, id, sinceId, maxId,
						FORMAT, MODE);
				break;
			case TYPE_STATUSES_CONTEXT_TIMELINE:
				if (App.DEBUG)
					Log.d(TAG, "fetchTimeline TYPE_CONTEXT");
				statuses = api.contextTimeline(id, FORMAT, MODE);
				break;
			default:
				break;
			}
			if (statuses == null || statuses.size() == 0) {
				sendIntMessage(0);
				if (App.DEBUG)
					Log.d(TAG, "fetchTimeline received no items.");
				return;
			} else {
				int size = statuses.size();
				if (App.DEBUG) {
					Log.d(TAG, "fetchTimeline received items count=" + size);
				}
				ContentResolver cr = getContentResolver();
				if (size >= count && page <= 1 && maxId == null) {
					String where = BasicColumns.TYPE + " = ?";
					String[] whereArgs = new String[] { String.valueOf(type) };
					int delete = cr.delete(StatusInfo.CONTENT_URI, where,
							whereArgs);
					if (App.DEBUG) {
						Log.d(TAG, "fetchTimeline items count = " + count
								+ " ,remove " + delete + " old statuses.");
					}
				}
				int insertedCount = cr.bulkInsert(StatusInfo.CONTENT_URI,
						Parser.toContentValuesArray(statuses));
				sendIntMessage(insertedCount);
				updateUsersFromStatus(statuses, type);
			}
		} catch (ApiException e) {
			if (App.DEBUG) {
				log("fetchTimeline [error]" + e.statusCode + ":"
						+ e.errorMessage);
				e.printStackTrace();
			}
			sendErrorMessage(e);
		}
	}

	private int updateUsersFromStatus(List<Status> statuses, int type) {
		if (type == TYPE_STATUSES_USER_TIMELINE || type == TYPE_FAVORITES_LIST) {
			return 0;
		}
		ArrayList<User> us = new ArrayList<User>();
		for (Status s : statuses) {
			User u = s.user;
			if (u != null) {
				if (!FanFouProvider.updateUserInfo(this, u)) {
					if (App.DEBUG) {
						log("extractUsers from status list , udpate failed, insert it");
					}
					us.add(s.user);
				}
			}
		}

		int result = 0;
		if (us.size() > 0) {
			result = getContentResolver().bulkInsert(UserInfo.CONTENT_URI,
					Parser.toContentValuesArray(us));
			if (App.DEBUG) {
				log("extractUsers from status list , insert result=" + result);
			}
		}
		return result;
	}

	public static void doFetchHomeTimeline(Context context,
			final ResultReceiver receiver, String sinceId, String maxId) {
		doFetchTimeline(context, TYPE_STATUSES_HOME_TIMELINE, receiver, 0,
				null, sinceId, maxId);
	}

	public static void doFetchMentions(Context context,
			final ResultReceiver receiver, String sinceId, String maxId) {
		doFetchTimeline(context, TYPE_STATUSES_MENTIONS, receiver, 0, null,
				sinceId, maxId);
	}

	public static void doFetchUserTimeline(Context context,
			final ResultReceiver receiver, String userId, String sinceId,
			String maxId) {
		doFetchTimeline(context, TYPE_STATUSES_USER_TIMELINE, receiver, 0,
				userId, sinceId, maxId);
	}

	public static void doFetchPublicTimeline(Context context,
			final ResultReceiver receiver) {
		doFetchTimeline(context, TYPE_STATUSES_PUBLIC_TIMELINE, receiver, 0,
				null, null, null);
	}

	public static void doFetchFavorites(Context context,
			final ResultReceiver receiver, int page, String userId) {
		doFetchTimeline(context, TYPE_FAVORITES_LIST, receiver, page, userId,
				null, null);
	}

	private static void doFetchTimeline(Context context, int type,
			final ResultReceiver receiver, int page, String userId,
			String sinceId, String maxId) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, type);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		intent.putExtra(EXTRA_COUNT, MAX_TIMELINE_COUNT);
		intent.putExtra(EXTRA_PAGE, page);
		intent.putExtra(EXTRA_ID, userId);
		intent.putExtra(EXTRA_SINCE_ID, sinceId);
		intent.putExtra(EXTRA_MAX_ID, maxId);
		if (App.DEBUG) {
			Log.d(TAG, "doFetchTimeline() type=" + type + " page=" + page
					+ " userId=" + userId);
		}
		context.startService(intent);
	}

	public static void doFetchFriends(Context context,
			final ResultReceiver receiver, int page, String userId) {
		doFetchUsers(context, TYPE_USERS_FRIENDS, receiver, page, userId);
	}

	public static void doFetchFollowers(Context context,
			final ResultReceiver receiver, int page, String userId) {
		doFetchUsers(context, TYPE_USERS_FOLLOWERS, receiver, page, userId);
	}

	private static void doFetchUsers(Context context, int type,
			final ResultReceiver receiver, int page, String userId) {
		Intent intent = new Intent(context, FanFouService.class);
		intent.putExtra(EXTRA_TYPE, type);
		intent.putExtra(EXTRA_RECEIVER, receiver);
		intent.putExtra(EXTRA_COUNT, MAX_USERS_COUNT);
		intent.putExtra(EXTRA_PAGE, page);
		intent.putExtra(EXTRA_ID, userId);
		context.startService(intent);
	}

	private void sendErrorMessage(ApiException e) {
		if (receiver != null) {
			String message = e.getMessage();
			if (e.statusCode == ResponseCode.ERROR_IO_EXCEPTION) {
				message = getString(R.string.msg_connection_error);
			} else if (e.statusCode >= 500) {
				message = getString(R.string.msg_server_error);
			}
			Bundle data = new Bundle();
			data.putInt(EXTRA_CODE, e.statusCode);
			data.putString(EXTRA_ERROR, message);
			sendMessage(RESULT_ERROR, data);
		}
	}

	private void sendIntMessage(int size) {
		if (receiver != null) {
			Bundle update = new Bundle();
			update.putInt(EXTRA_COUNT, size);
			sendMessage(RESULT_SUCCESS, update);
		}
	}

	private void sendParcelableMessage(Parcelable parcel) {
		if (receiver != null) {
			Bundle data = new Bundle();
			data.putParcelable(EXTRA_DATA, parcel);
			sendMessage(RESULT_SUCCESS, data);
		}
	}

	private void sendSuccessMessage(Bundle data) {
		if (receiver != null) {
			sendMessage(RESULT_SUCCESS, data);
		}
	}

	private void sendSuccessMessage() {
		if (receiver != null) {
			sendMessage(RESULT_SUCCESS, new Bundle());
		}
	}

	private void sendMessage(int code, Bundle data) {
		if (data == null) {
			throw new NullPointerException(
					"sendSuccessMessage() bundle cannot be bull.");
		}
		if (receiver != null) {
			data.putInt(EXTRA_TYPE, type);
			receiver.send(code, data);
		}
	}

	private static void onSuccess(ResultListener li, int type, String message) {
		if (li != null) {
			li.onActionSuccess(type, message);
		}
	}

	private static void onFailed(ResultListener li, int type, String message) {
		if (li != null) {
			li.onActionFailed(type, message);
		}
	}

}
