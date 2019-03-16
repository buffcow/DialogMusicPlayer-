				/**
		     *Dialog样式的简单音乐播放器
				 */
				//播放器变量
				private final SimpleDateFormat sdf = new SimpleDateFormat("m:ss");
				private boolean isSeekBarChanging = false;
				private AlertDialog mAlertDialog = null;
				private boolean isComplete = false;
				private MediaPlayer mp;
				private Handler mHandler = new Handler(){ //设置一个Handle来更新信息
						@Override
						public void handleMessage(Message msg) {
								int what = msg.what;
								SeekBar seekBar = mAlertDialog.findViewById(R.id.seekbar); //进度条
								TextView time = mAlertDialog.findViewById(R.id.time); //时间
								if (what == 0) {	//update
										if (!isSeekBarChanging && mp != null) { //如果进度条不滑动以及播放器不为空时，更新信息
												if (!isComplete) { //是否完成了播放
														// 更新时间信息，显示音乐当前所在时长
														time.setText(sdf.format(mp.getCurrentPosition()) + "/" + sdf.format(mp.getDuration()));
														seekBar.setProgress(mp.getCurrentPosition()); //更新进度条，显示当前进度
														mHandler.sendEmptyMessageDelayed(0, 100); //循环发送消息，达到自动更新信息的目的
												} else { //完成了播放
														seekBar.setProgress(0); //设置进度条的进度为零
														time.setText(sdf.format(0) + "/" + sdf.format(mp.getDuration())); //设置当前时间为00:00
														mAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText("重新播放"); //重新设置Negative按钮文本
												}
										}
								}
						}
				};
				//播放器方法
				private final void playMusic(HashMap<String, Object> hm, final String music) {
						mp = new MediaPlayer(); //这句很必要，用来实例化播放器，否则报空指针
						try {
								isComplete = false; //播放未完成
								mp.setDataSource(music); //音频文件
								mp.prepare(); //准备
								mp.start(); //播放
						} catch (Exception e) {
								// 播放错误提示
								MainActivity.setFloatMsg("E播放失败：" + e.toString(), 3000);
								return;
						}
						//整个播放器弹窗布局
						final View v = LayoutInflater.from(MainActivity.mContext).inflate(R.layout.layout_player_dialog, null);
						final TextView name = v.findViewById(R.id.name); //音频文件名View
						final SeekBar seekbar = v.findViewById(R.id.seekbar); //进度条View
						final TextView time = v.findViewById(R.id.time); //时间View
						name.setText((String)hm.get("name")); //设置文件名
						seekbar.setMax(mp.getDuration()); //设置进度条的最大值，即音频文件的总时长
						//设置进度条滑动时间
						seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
										//进度条变化时调用
										@Override
										public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
												if (fromUser) { //被动式滑动
														//设置时间为当前滑动的时间，实现滑动更新时间
														time.setText(sdf.format(progress) + "/" + sdf.format(mp.getDuration()));
														isSeekBarChanging = true; //进度条正在滑动
														//mHandler.sendEmptyMessage(0); //发送消息
												}
										}

										//进度条滑动时调用
										@Override
										public void onStartTrackingTouch(SeekBar seekBar) {
												isSeekBarChanging = true; //进度条正在滑动
										}

										//进度条停止滑动时调用
										@Override
										public void onStopTrackingTouch(SeekBar seekBar) {
												isSeekBarChanging = false; //进度条停止滑动
												mp.seekTo(seekBar.getProgress()); //播放器设置进度条当前值
												mHandler.sendEmptyMessage(0); //发送消息，此句不可省，否则无法自动更新信息
										}
								});

						//实例化一个弹窗
						mAlertDialog = new AlertDialog.Builder(MainActivity.mContext)
								.setView(v) //设置布局
								//同意按钮
								.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
												//点击后保持dialog关闭
												try {
														mAlertDialog.dismiss();
														java.lang.reflect.Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
														field.setAccessible(true);
														field.set(dialog, true);
												} catch (Exception e) {
														//错误提示
														MainActivity.setFloatMsg("E" + e.toString(), 3000);
														return;
												}
												mp.stop(); //停止播放
												mp.release(); //释放
												mp = null; // 回收
												new File(music).delete(); //删除缓存文件
										}
								})
								//拒绝按钮
								.setNegativeButton("暂停", new DialogInterface.OnClickListener(){
										public void onClick(DialogInterface dialog, int which) {
												//点击后保持dialog不关闭
												try {
														java.lang.reflect.Field field = dialog.getClass().getSuperclass().getDeclaredField("mShowing");
														field.setAccessible(true);
														field.set(dialog, false);
												} catch (Exception e) {
														//错误提示
														MainActivity.setFloatMsg("E" + e.toString(), 3000);
														return;
												}
												if (mp.isPlaying()) { //播放器正在播放
														mp.pause(); //暂停
														mAlertDialog.getButton(which).setText("播放"); //更新按钮文本
												} else { //播放器不在播放
														mp.start(); //播放
														mAlertDialog.getButton(which).setText("暂停"); //更新按钮文本
														mHandler.sendEmptyMessage(0); //发送消息，自动更新信息
												}
												isComplete = false; //点击后，未完成播放
										}
								})
								.create(); //构建弹窗
						mAlertDialog.setCancelable(false); //返回键不可关闭
						mAlertDialog.setCanceledOnTouchOutside(false); //点击外部不可关闭
						mAlertDialog.setTitle("KGE播放器"); //弹窗标题
						mAlertDialog.show(); //显示弹窗
						//弹窗消失时调用
						mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
										public void onDismiss(DialogInterface dialog) {
												if (mp != null) { //播放器变量不为空
														mp.stop(); //停止
														mp.release(); //释放
														mp = null; //回收
												}
												new File(music).delete(); //删除缓存文件
										}
								});
						//播放器播放完成时调用
						mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
										public void onCompletion(MediaPlayer mPlayer) {
												if (isSeekBarChanging) //进度条滑动时忽略
														return;
												isComplete = true; //完成播放了
										}
								});
				}