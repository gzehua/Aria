/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arialyy.aria.core.download;

import android.text.TextUtils;
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import com.arialyy.annotations.TaskEnum;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.command.CancelAllCmd;
import com.arialyy.aria.core.command.NormalCmdFactory;
import com.arialyy.aria.core.common.AbsBuilderTarget;
import com.arialyy.aria.core.common.ProxyHelper;
import com.arialyy.aria.core.event.EventMsgUtil;
import com.arialyy.aria.core.inf.AbsEntity;
import com.arialyy.aria.core.inf.AbsReceiver;
import com.arialyy.aria.core.inf.ITask;
import com.arialyy.aria.core.inf.ReceiverType;
import com.arialyy.aria.core.scheduler.TaskSchedulers;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import com.arialyy.aria.util.DbDataHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by lyy on 2016/12/5.
 * 下载功能接收器
 */
public class DownloadReceiver extends AbsReceiver {
  private final String TAG = "DownloadReceiver";

  /**
   * 设置最大下载速度，单位：kb
   *
   * @param maxSpeed 为0表示不限速
   * @deprecated {@code Aria.get(Context).getDownloadConfig().setMaxSpeed(int)}
   */
  @Deprecated
  public DownloadReceiver setMaxSpeed(int maxSpeed) {
    AriaManager.getInstance().getDownloadConfig().setMaxSpeed(maxSpeed);
    return this;
  }

  /**
   * 加载Http、https单任务下载地址，用于任务第一次下载，如果需要控制任务停止或删除等操作，请使用{@link #load(long)}
   *
   * @param url 下载地址
   */
  @CheckResult
  public HttpBuilderTarget load(@NonNull String url) {
    CheckUtil.checkUrlInvalidThrow(url);
    return new HttpBuilderTarget(url, targetName);
  }

  /**
   * 用于任务停止、删除等操作
   *
   * @param taskId 任务id，可从{@link AbsBuilderTarget#create()}、{@link AbsBuilderTarget#add()}、{@link
   * AbsEntity#getId()}读取任务id
   */
  @CheckResult
  public HttpNormalTarget load(long taskId) {
    CheckUtil.checkTaskId(taskId);
    return new HttpNormalTarget(taskId, targetName);
  }

  /**
   * 加载组合任务，用于任务第一次下载，如果需要控制任务停止或删除等操作，请使用{@link #loadGroup(long)}
   *
   * @param urls 组合任务只任务列被，如果任务组的中的下载地址改变了，则任务从新的一个任务组
   */
  @CheckResult
  public GroupBuilderTarget loadGroup(List<String> urls) {
    CheckUtil.checkDownloadUrls(urls);
    return new GroupBuilderTarget(urls, targetName);
  }

  /**
   * 加载组合任务，用于任务停止、删除等操作
   *
   * @param taskId 任务id，可从{@link AbsBuilderTarget#create()}、{@link AbsBuilderTarget#add()}、{@link
   * * AbsEntity#getId()}读取任务id
   */
  @CheckResult
  public GroupNormalTarget loadGroup(long taskId) {
    CheckUtil.checkTaskId(taskId);
    return new GroupNormalTarget(taskId, targetName);
  }

  /**
   * 加载ftp单任务下载地址，用于任务第一次下载，如果需要控制任务停止或删除等操作，请使用{@link #loadFtp(long)}
   */
  @CheckResult
  public FtpBuilderTarget loadFtp(@NonNull String url) {
    CheckUtil.checkUrlInvalidThrow(url);
    return new FtpBuilderTarget(url, targetName);
  }

  /**
   * 用于任务停止、删除等操作
   *
   * @param taskId 任务id，可从{@link AbsBuilderTarget#create()}、{@link AbsBuilderTarget#add()}、{@link
   * AbsEntity#getId()}读取任务id
   */
  @CheckResult
  public FtpNormalTarget loadFtp(long taskId) {
    CheckUtil.checkTaskId(taskId);
    return new FtpNormalTarget(taskId, targetName);
  }

  /**
   * 加载ftp文件夹下载地址，用于任务第一次下载，如果需要控制任务停止或删除等操作，请使用{@link #loadFtpDir(long)}
   */
  @CheckResult
  public FtpDirBuilderTarget loadFtpDir(@NonNull String dirUrl) {
    CheckUtil.checkUrlInvalidThrow(dirUrl);
    return new FtpDirBuilderTarget(dirUrl, targetName);
  }

  /**
   * 加载ftp文件夹下载地址，用于任务停止、删除等操作
   *
   * @param taskId 任务id，可从{@link AbsBuilderTarget#create()}、{@link AbsBuilderTarget#add()}、{@link
   * AbsEntity#getId()}读取任务id
   */
  @CheckResult
  public FtpDirNormalTarget loadFtpDir(long taskId) {
    CheckUtil.checkTaskId(taskId);
    return new FtpDirNormalTarget(taskId, targetName);
  }

  /**
   * 将当前类注册到Aria
   */
  public void register() {
    if (TextUtils.isEmpty(targetName)) {
      ALog.e(TAG, "download register target null");
      return;
    }
    Object obj = OBJ_MAP.get(getKey());
    if (obj == null) {
      ALog.e(TAG, String.format("【%s】观察者为空", targetName));
      return;
    }
    Set<Integer> set = ProxyHelper.getInstance().checkProxyType(obj.getClass());
    if (set != null && !set.isEmpty()) {
      for (Integer type : set) {
        if (type == ProxyHelper.PROXY_TYPE_DOWNLOAD) {
          TaskSchedulers.getInstance().register(obj, TaskEnum.DOWNLOAD);
        } else if (type == ProxyHelper.PROXY_TYPE_DOWNLOAD_GROUP) {
          TaskSchedulers.getInstance().register(obj, TaskEnum.DOWNLOAD_GROUP);
        } else if (type == ProxyHelper.PROXY_TYPE_M3U8_PEER) {
          TaskSchedulers.getInstance().register(obj, TaskEnum.M3U8_PEER);
        } else if (type == ProxyHelper.PROXY_TYPE_DOWNLOAD_GROUP_SUB) {
          TaskSchedulers.getInstance().register(obj, TaskEnum.DOWNLOAD_GROUP_SUB);
        }
      }
    } else {
      ALog.w(TAG, "没有Aria的注解方法");
    }
  }

  /**
   * 取消注册，如果是Activity或fragment，Aria会界面销毁时自动调用该方法，不需要你手动调用。
   * 注意事项：
   * 1、如果在activity中一定要调用该方法，那么请在{@code onDestroy()}中调用
   * 2、不要在activity的{@code onPreStop()}中调用改方法
   * 3、如果是Dialog或popupwindow，需要你在撤销界面时调用该方法
   * 4、如果你是在Module（非android组件类）中注册了Aria，那么你也需要在Module类中调用该方法，而不是在组件类中
   * 调用销毁，详情见
   *
   * @see <a href="https://aria.laoyuyu.me/aria_doc/start/any_java.html#%E6%B3%A8%E6%84%8F%E4%BA%8B%E9%A1%B9">module类中销毁</a>
   */
  @Override public void unRegister() {
    if (needRmListener) {
      unRegisterListener();
    }
    AriaManager.getInstance().removeReceiver(OBJ_MAP.get(getKey()));
  }

  @Override public String getType() {
    return ReceiverType.DOWNLOAD;
  }

  @Override protected void unRegisterListener() {
    if (TextUtils.isEmpty(targetName)) {
      ALog.e(TAG, "download unRegisterListener target null");
      return;
    }
    Object obj = OBJ_MAP.get(getKey());
    if (obj == null) {
      ALog.e(TAG, String.format("【%s】观察者为空", targetName));
      return;
    }
    Set<Integer> set = ProxyHelper.getInstance().mProxyCache.get(obj.getClass().getName());
    if (set != null) {
      for (Integer integer : set) {
        if (integer == ProxyHelper.PROXY_TYPE_DOWNLOAD) {
          TaskSchedulers.getInstance().unRegister(obj);
        } else if (integer == ProxyHelper.PROXY_TYPE_DOWNLOAD_GROUP) {
          TaskSchedulers.getInstance().unRegister(obj);
        }
      }
    }
  }

  /**
   * 获取任务实体
   *
   * @param taskId 任务实体唯一id
   */
  public DownloadEntity getDownloadEntity(long taskId) {
    CheckUtil.checkTaskId(taskId);
    return DbEntity.findFirst(DownloadEntity.class, "rowid=?", String.valueOf(taskId));
  }

  /**
   * 获取第一个匹配url的下载实体，如果你有多个任务的下载地址都相同，请使用{@link #getDownloadEntity(long)}
   * 或{@link #getDownloadEntity(String)}
   *
   * @return 如果url错误或查找不到数据，则返回null
   */
  public DownloadEntity getFirstDownloadEntity(String downloadUrl) {
    CheckUtil.checkUrl(downloadUrl);
    return DbEntity.findFirst(DownloadEntity.class, "url=? and isGroupChild='false'", downloadUrl);
  }

  /**
   * 通过下载链接获取下载实体
   *
   * @return 如果url错误或查找不到数据，则返回null
   */
  public List<DownloadEntity> getDownloadEntity(String downloadUrl) {
    CheckUtil.checkUrl(downloadUrl);
    return DbEntity.findDatas(DownloadEntity.class, "url=? and isGroupChild='false'", downloadUrl);
  }

  /**
   * 获取组合任务实在实体
   *
   * @param taskId 组合任务实体id
   * @return 如果实体不存在，返回null
   */
  public DownloadGroupEntity getGroupEntity(long taskId) {
    CheckUtil.checkTaskId(taskId);
    return DbDataHelper.getDGEntity(taskId);
  }

  /**
   * 获取组合任务实在实体
   *
   * @param urls 组合任务的url
   * @return 如果实体不存在，返回null
   */
  public DownloadGroupEntity getGroupEntity(List<String> urls) {
    CheckUtil.checkDownloadUrls(urls);
    return DbDataHelper.getDGEntity(CommonUtil.getMd5Code(urls));
  }

  /**
   * 获取Ftp文件夹任务的实体
   *
   * @param url ftp文件夹下载路径
   * @return 如果实体不存在，返回null
   */
  public DownloadGroupEntity getFtpDirEntity(String url) {
    CheckUtil.checkUrl(url);
    return DbDataHelper.getDGEntity(url);
  }

  /**
   * 下载任务是否存在
   *
   * @return {@code true}存在，{@code false} 不存在
   */
  public boolean taskExists(String downloadUrl) {
    return DbEntity.checkDataExist(DownloadEntity.class, "url=?", downloadUrl);
  }

  /**
   * 判断任务组是否存在
   *
   * @return {@code true} 存在；{@code false} 不存在
   */
  public boolean taskExists(List<String> urls) {
    if (urls == null || urls.isEmpty()) {
      return false;
    }
    String groupHash = CommonUtil.getMd5Code(urls);
    return DbEntity.checkDataExist(DownloadGroupEntity.class, "groupHash=?", groupHash);
  }

  /**
   * 获取所有普通下载任务
   * 获取未完成的普通任务列表{@link #getAllNotCompleteTask()}
   * 获取已经完成的普通任务列表{@link #getAllCompleteTask()}
   */
  public List<DownloadEntity> getTaskList() {
    return DbEntity.findDatas(DownloadEntity.class, "isGroupChild=? and downloadPath!=''",
        "false");
  }

  /**
   * 分页获取所有普通下载任务
   * 获取未完成的普通任务列表{@link #getAllNotCompleteTask()}
   * 获取已经完成的普通任务列表{@link #getAllCompleteTask()}
   *
   * @param page 当前页，不能小于1
   * @param num 每页数量，不能小于1
   * @return 如果页数大于总页数，返回null
   */
  public List<DownloadEntity> getTaskList(int page, int num) {
    CheckUtil.checkPageParams(page, num);
    return DbEntity.findDatas(DownloadEntity.class, page, num,
        "isGroupChild=? and downloadPath!=''", "false");
  }

  /**
   * 获取所有未完成的普通下载任务
   */
  public List<DownloadEntity> getAllNotCompleteTask() {
    return DbEntity.findDatas(DownloadEntity.class,
        "isGroupChild=? and downloadPath!='' and isComplete=?", "false", "false");
  }

  /**
   * 分页获取所有未完成的普通下载任务
   *
   * @param page 当前页，不能小于1
   * @param num 每页数量，不能小于1
   * @return 如果页数大于总页数，返回null
   */
  public List<DownloadEntity> getAllNotCompleteTask(int page, int num) {
    CheckUtil.checkPageParams(page, num);
    return DbEntity.findDatas(DownloadEntity.class, page, num,
        "isGroupChild=? and downloadPath!='' and isComplete=?", "false", "false");
  }

  /**
   * 获取所有已经完成的普通任务
   */
  public List<DownloadEntity> getAllCompleteTask() {
    return DbEntity.findDatas(DownloadEntity.class,
        "isGroupChild=? and downloadPath!='' and isComplete=?", "false", "true");
  }

  /**
   * 分页获取所有已经完成的普通任务
   *
   * @param page 当前页，不能小于1
   * @param num 每页数量，不能小于1
   * @return 如果页数大于总页数，返回null
   */
  public List<DownloadEntity> getAllCompleteTask(int page, int num) {
    CheckUtil.checkPageParams(page, num);
    return DbEntity.findDatas(DownloadEntity.class,
        "isGroupChild=? and downloadPath!='' and isComplete=?", "false", "true");
  }

  /**
   * 获取任务组列表
   *
   * @return 如果没有任务组列表，则返回null
   */
  public List<DownloadGroupEntity> getGroupTaskList() {
    List<DGEntityWrapper> wrappers = DbEntity.findRelationData(DGEntityWrapper.class);
    if (wrappers == null || wrappers.isEmpty()) {
      return null;
    }
    List<DownloadGroupEntity> entities = new ArrayList<>();
    for (DGEntityWrapper wrapper : wrappers) {
      entities.add(wrapper.groupEntity);
    }
    return entities;
  }

  /**
   * 分页获取祝贺任务列表
   *
   * @param page 当前页，不能小于1
   * @param num 每页数量，不能小于1
   * @return 如果没有任务组列表，则返回null
   */
  public List<DownloadGroupEntity> getGroupTaskList(int page, int num) {
    List<DGEntityWrapper> wrappers = DbEntity.findRelationData(DGEntityWrapper.class, page, num);
    if (wrappers == null || wrappers.isEmpty()) {
      return null;
    }
    List<DownloadGroupEntity> entities = new ArrayList<>();
    for (DGEntityWrapper wrapper : wrappers) {
      entities.add(wrapper.groupEntity);
    }
    return entities;
  }

  /**
   * 获取普通任务和任务组的任务列表
   */
  public List<AbsEntity> getTotalTaskList() {
    List<AbsEntity> list = new ArrayList<>();
    List<DownloadEntity> simpleTask = getTaskList();
    List<DownloadGroupEntity> groupTask = getGroupTaskList();
    if (simpleTask != null && !simpleTask.isEmpty()) {
      list.addAll(simpleTask);
    }
    if (groupTask != null && !groupTask.isEmpty()) {
      list.addAll(groupTask);
    }
    return list;
  }

  /**
   * 停止所有正在下载的任务，并清空等待队列。
   */
  public void stopAllTask() {
    EventMsgUtil.getDefault().post(NormalCmdFactory.getInstance()
        .createCmd(new DTaskWrapper(null), NormalCmdFactory.TASK_STOP_ALL,
            ITask.DOWNLOAD));
  }

  /**
   * 恢复所有正在下载的任务
   * 1.如果执行队列没有满，则开始下载任务，直到执行队列满
   * 2.如果队列执行队列已经满了，则将所有任务添加到等待队列中
   */
  public void resumeAllTask() {
    EventMsgUtil.getDefault().post(NormalCmdFactory.getInstance()
        .createCmd(new DTaskWrapper(null), NormalCmdFactory.TASK_RESUME_ALL,
            ITask.DOWNLOAD));
  }

  /**
   * 删除所有任务
   *
   * @param removeFile {@code true} 删除已经下载完成的任务，不仅删除下载记录，还会删除已经下载完成的文件，{@code false}
   * 如果文件已经下载完成，只删除下载记录
   */
  public void removeAllTask(boolean removeFile) {
    final AriaManager ariaManager = AriaManager.getInstance();
    CancelAllCmd cancelCmd =
        (CancelAllCmd) CommonUtil.createNormalCmd(new DTaskWrapper(null),
            NormalCmdFactory.TASK_CANCEL_ALL, ITask.DOWNLOAD);
    cancelCmd.removeFile = removeFile;
    EventMsgUtil.getDefault().post(cancelCmd);

    Set<String> keys = ariaManager.getReceiver().keySet();
    for (String key : keys) {
      ariaManager.getReceiver().remove(key);
    }
  }
}