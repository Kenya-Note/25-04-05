import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Parameter {

	// シード値の基本設定
	private long baseSeed;
	private boolean useFixedSeed; // 固定シードを使用するかどうか

	// シミュレーション期間
	private int t_end;
	// エージェント数
	private int agentNum;
	// 重みの最大値
	private double u_max;
	// 乱数の最大値
	private int tau_max;
	// ファンダメンタル価格
	private double p_fund;
	// 重み再設定確率
	private double m;
	// ティックサイズ
	private double delta_p;
	// ファンダメンタル成分の最大値
	private double w1_max;
	// テクニカル成分最大値
	private double w2_max;
	// 予想リターン計算乱数
	private double sigma_e;
	// 注文価格のばらつき係数
	private double est;
	// ファンダメンタル学習参照期間
	private int n;
	// 学習参照期間
	private int t_l;
	// 学習係数
	private double k_l;
	// 注文有効期間
	private int t_c;
	// 板形成期間
	private int t_bookMake;
	// シミュレーション開始価格
	private double p_start;
	// 1日あたりの期間
	private int dayPeriod;
	// 表示インターバル
	private int dispInterval;
	// マーケットメイカー固有のスプレッド
	private double thetaHFT;
	// ポジション考慮度
	private double w_pm;
	
	// 2021/3/30 
	//NA連続成行（売り）注文開始期
	private int cnt_mrkt_ordr_strt ;
	//NA連続成行（売り）注文期間
	private int cnt_mrkt_ordr_prd ;
	//NA連続成行（売り）注文確率
	private double cnt_mrkt_ordr_pr ;
	
	// 2021/12/16 
	// 見せ玉
	private boolean spoofer_flag;
	private int numberofSpoofer ;
	private static int[] arraySpoofer = {0, 10, 100, 200, 1000} ;
	private int subSpoofer;
	private int spoofer_interval;
	private int spoofer_strt;

	// 21/4/27 オーダーインバランス考慮度
	private double w_om ;

	// パラメータ配列
	private static double[] arrayDelta_p = {0.01, 0.1, 1.0, 10, 100};
	private static double[] arrayW1_max = {1.0, 3.0, 5.0, 8.0, 10.0};
	private static double[] arrayW2_max= {1.0, 3.0, 5.0, 8.0, 10.0};
	private static double[] arraySigma_e = {0.02, 0.04, 0.06, 0.08, 0.1};
	private static double[] arrayEst = {0.003, 0.005, 0.01, 0.02, 0.03};
	private static int[] arrayT_c = {10000, 15000, 20000, 25000, 30000, 60000};

	// パラメータ配列添字
	private int subDelta_p;
	private int subW1_max;
	private int subW2_max;
	private int subSigma_e;
	private int subEst;
	private int subT_c;

	// 期間別注文数の集計に使用
	private int d_pf;
	
	// カウント用の変数
	private int i;

	public Parameter(int i) {
		// シード値の初期化
		this.baseSeed = 12345L; // 基本シード値
		this.useFixedSeed = true; // 再現性のために固定シードを使用

		this.t_end = 2000000;	// シミュレーションの終了期
		this.agentNum = 1100; // NormalAgent1000体+FundamentalAgent100体 
		this.u_max = 1;
		this.tau_max = 10000;
		this.p_fund = 10000;
		this.m = 0.01; // 25/04/05 ノーマルエージェントの学習機能を切る
		this.n = 1000;
		this.t_l = 10000;
		this.k_l = 4.0;
		this.i = i;
	
		// 板形成期間
		this.t_bookMake = 20000;
		// シミュレーション開始価格
		this.p_start = this.p_fund;
		// 1日あたりの期間
		this.dayPeriod = 20000;
		// 表示インターバル
		this.dispInterval = 20000;
		
		/*
		// 板形成期間
		this.t_bookMake = 40000;
		// シミュレーション開始価格
		this.p_start = this.p_fund;
		// 1日あたりの期間
		this.dayPeriod = 40000;
		// 表示インターバル
		this.dispInterval = 20000;
		*/
		
		// HFTエージェント固有のスプレッド
		thetaHFT = 0.0003;
		// HFTの資産ポジション考慮度
//		w_pm = 0.0000000005; // 1/100
//		w_pm = 0.000000005;  // 1/10
		w_pm = 0.00000005;   // 基準値
//		w_pm = 0.0000005;    // 10倍
//		w_pm = 0.000005;     // 100倍

		// 21/4/27 HFTのオーダーインバランス考慮度
//		w_om = 0.000000005;	// 1/10
//		w_om = 0.00000005;	// 基準値
//		w_om = 0.0000005;	// 10倍
//		w_om = 0.000005;	// 100倍
		w_om = 10.0;

		// 2021/3/30 
		//NA連続成行（売り）注文開始期
		this.cnt_mrkt_ordr_strt = 40000000;
		//NA連続成行（売り）注文期間
		this.cnt_mrkt_ordr_prd = 40000000;
		//NA連続成行（売り）注文確率
		this.cnt_mrkt_ordr_pr = 0.2 ;

		// 添字初期
		this.subDelta_p = 0; // ティックサイズ 0.01
		this.subW1_max = 0; 
		this.subW2_max = 4;
		// this.subSigma_e = 2; 
		this.subEst = 0;  
		this.subT_c = 0; 

		// パラメータの初期
		this.delta_p = arrayDelta_p[this.subDelta_p];
		this.w1_max = arrayW1_max[this.subW1_max];
		this.w2_max = arrayW2_max[this.subW2_max];
		// this.sigma_e = arraySigma_e[this.subSigma_e];
		this.sigma_e = 0.03;
		this.est = arrayEst[this.subEst];
		this.t_c = arrayT_c[this.subT_c];
		
		// 21/12/16 見せ玉の設定
		this.spoofer_flag = false;
		this.subSpoofer = 4;	// 見せ玉の設定
		this.numberofSpoofer = arraySpoofer[this.subSpoofer]; //arraySpoofer[5] = {0, 10, 100, 200, 1000} ;
		this.spoofer_interval = 100000;
		this.spoofer_strt = 100000;

		this.d_pf = 50;
	
	}

	// 一旦、シード値に関するメソッドを記述
	public long getSeedForAgent(int agentID) {
		if (useFixedSeed) {
			return baseSeed + agentID; // 固定シードを基にしつつ、エージェントごとに異なる値を生成
		} else {
			return System.nanoTime() + agentID;
		}
	}

	public void setBaseSeed(long seed){
		this.baseSeed = seed;
	}

	public void setUseFixedSeed(boolean useFixed) {
		this.useFixedSeed = useFixed;
	}

	public long getBaseSeed() {
		return this.baseSeed;
	}

	public boolean isUsingFixedSeed() {
		return this.useFixedSeed;
	}

	// defaultに設定（差しあたっては未使用）
	public void setDefault() {
		// default
		updateDelta_p(2); //1.0		{0.01, 0.1, 1.0, 10, 100};
		updateW1_max(0);  //1.0		{1.0, 3.0, 5.0, 8.0, 10.0};
		updateW2_max(4);  //10.0	{1.0, 3.0, 5.0, 8.0, 10.0};
		updateSigma_e(2); //0.06	{0.02, 0.04, 0.06, 0.08, 0.1};
		updateEst(4);     //0.03	{0.003, 0.005, 0.01, 0.02, 0.03};
		updateT_c(0);     //10000	{10000, 15000, 20000, 25000, 30000};
		
		// 21/12/16
		updateSpoofer(0); //0 		{0, 10, 100, 1000, 10000} ;
	}

	// パラメータの更新（差しあたっては未使用）
	public void updateDelta_p(int n) {
		subDelta_p = n;
		delta_p = arrayDelta_p[subDelta_p];
	}
	public void updateW1_max(int n) {
		subW1_max = n;
		w1_max = arrayW1_max[subW1_max];
	}
	public void updateW2_max(int n) {
		subW2_max = n;
		w2_max = arrayW2_max[subW2_max];
	}
	public void updateSigma_e(int n) {
		subSigma_e = n;
		sigma_e = arraySigma_e[subSigma_e];
	}
	public void updateEst(int n) {
		subEst = n;
		est = arrayEst[subEst];
	}
	public void updateT_c(int n) {
		subT_c = n;
		t_c = arrayT_c[subT_c];
	}
	// 21/12/16
	public void updateSpoofer(int n) {
		subSpoofer = n;
		numberofSpoofer = arraySpoofer[subSpoofer];
	}
	
	// パラメータの表示 
	// 21/4/26 hft_flag変更
	public void dispParam(int appNum, Flag.HFT hft_flag) {

		// 21/429 if -> switch
		switch (hft_flag) {
			case NONE:
				System.out.print("ノーマルエージェントのみ ");
				break;
			case SMM:
				System.out.print("SMM参加 ");
				break;
			case nPMM:
				System.out.print("PMM（成行注文なし）参加 ");
				break;
			case mPMM:
				System.out.print("PMM（成行注文なし）参加 ");
				break;
			case OMM:
				System.out.print("OMM参加 ");
				break;
			case POMM:
				System.out.print("POMM参加 ");
				break;
			case PrOMM:	// 21/6/27 追加
				System.out.print("PrOMM参加 ");
				break;
		}
//		if(hft_flag != HFT_flag.NONE) {
//			System.out.print(appNum + "#");
//		}
//		else {
//			System.out.print(appNum + "@");
//		}
		System.out.print("delta_p: " + this.getDelta_p());
		System.out.print(", est: " + this.getEst());
		System.out.print(", sigma: " + this.getSigma_e());
		System.out.print(", t_c: " + this.getT_c());
		System.out.print(", w1: " + this.getW1_max());
		System.out.print(", w2: " + this.getW2_max() + "\n");
	}

	// 21/4/26 変更（boolean -> HFT_flag）
	public void exportParam(String folder, int appNum, Flag.HFT hft_flag) {
		FileWriter f;
		PrintWriter p;
		String separator = File.separator;
		
   	// 21/5/5 CSVファイル名に付加するエージェント種別
		String agent_type = hft_flag.toString() ;
		// 21/12/23アプリ別フォルダの廃止
//		String fileName = "Result_CSV" + separator + folder + separator + "app" + appNum + "_" + agent_type + separator  + "app" + appNum + "_" + agent_type + "_parameter.txt";
		String fileName = "Result_CSV" + separator + folder + separator + appNum + "_" + agent_type + "_parameter.txt";
		
		try {
			f = new FileWriter(fileName, false);
	    	p = new PrintWriter(new BufferedWriter(f));
	    	p.println("Simulation Parameter\n");
	    	// 21/4/26 変更
	    	if (hft_flag == Flag.HFT.NONE) p.println("HFT無し");
	    	else if (hft_flag == Flag.HFT.SMM) p.println("シンプルマーケットメイカー参加");
	    	else if (hft_flag == Flag.HFT.nPMM) p.println("ポジションマーケットメイカー（成行注文なし）参加");
	    	else if (hft_flag == Flag.HFT.mPMM) p.println("シンプルマーケットメイカー（成行注文あり）参加");
	    	else if (hft_flag == Flag.HFT.OMM) p.println("オーダーデプスインバランスマーケットメイカー参加");
	    	else if (hft_flag == Flag.HFT.POMM) p.println("オーダーデプスインバランス（順張り）ポジションマーケットメイカー参加");
	    	else if (hft_flag == Flag.HFT.PrOMM) p.println("オーダーデプスインバランス（逆張り）ポジションマーケットメイカー参加");
	

	    	p.println("============================================");
	    	p.println("// ティックサイズ");
	    	p.println("delta_p: " + this.getDelta_p() + "\n");
	    	p.println("// 注文価格のばらつき係数");
	    	p.println("est    : " + this.getEst() + "\n");
	    	p.println("// 予想リターンのばらつき");
	    	p.println("sigma  : " + this.getSigma_e() + "\n");
	    	p.println("// キャンセル期間");
	    	p.println("t_c    : " + this.getT_c() + "\n");
	    	p.println("// ファンダメンタル成分の重み");
	    	p.println("w1     : " + this.getW1_max() + "\n");
	    	p.println("// テクニカル成分の重み");
	    	p.println("w2     : " + this.getW2_max());
	    	p.println("============================================");
			// 書き込み終了
			p.close();
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// getter
	public double getDelta_p() {
		return this.delta_p;
	}
	public double getW1_max() {
		return this.w1_max;
	}
	public double getW2_max() {
		return this.w2_max;
	}
	public double getSigma_e() {
		return this.sigma_e;
	}
	public double getEst() {
		return this.est;
	}
	public int getT_c() {
		return this.t_c;
	}
	public int getAgentNum() {
		return this.agentNum;
	}
	public double getU_max() {
		return this.u_max;
	}
	public int getTau_max() {
		return this.tau_max;
	}
	public double getP_fund() {
		return this.p_fund;
	}
	public double getM() {
		return this.m;
	}
	public int getN() {
		return this.n;
	}
	public int getT_l() {
		return this.t_l;
	}
	public double getK_l() {
		return this.k_l;
	}
	public int getT_end() {
		return this.t_end;
	}
	public int getT_bookMake() {
		return this.t_bookMake;
	}
	public double getP_start() {
		return this.p_start;
	}
	public int getDayPeriod() {
		return this.dayPeriod;
	}
	public int getDispInterval() {
		return this.dispInterval;
	}
	public double getTheta() {
		return thetaHFT;
	}
	public double getW_pm() {
		return w_pm;
	}
	
	// 21/4/27
	public double getW_om() {
		return w_om ;
	}
	// 21/3/30
	public int getCnt_mrkt_ordr_strt() {
		return this.cnt_mrkt_ordr_strt ;
	}
	public int getCnt_mrkt_ordr_prd() {
		return this.cnt_mrkt_ordr_prd ;
	}
	public double getCnt_mrkt_ordr_pr() {
		return this.cnt_mrkt_ordr_pr ;
	}
	
	// 21/12/16
	public int getSpoofer_interval() {
		return this.spoofer_interval ;
	}
	public boolean getSpoofer_flag() {
		return this.spoofer_flag ;
	}
	public int getSpoofer_strt() {
		return this.spoofer_strt ;
	}
	public int getNumberofspoofer() {
		return this.numberofSpoofer ;
	}

	public int getd_pf() {
		return this.d_pf;
	}
	
	// カウント変数を返す
	public int getI() {
		return this.i;
	}
}