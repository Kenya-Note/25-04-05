import java.util.SplittableRandom;

public class NormalAgent extends Agent{

	// ファンダメンタル成分の重み
	private double w_fund;
	// テクニカル成分の重み
	private double w_tech;
	// シミュレーション中で不変の重み
	private double u;
	// ファンダメンタル投資家成分の予想リターン
	private double ret_fund;
	// テクニカル投資家成分の予想リターン
	private double ret_tech;
	// 正規分布または一様乱数
	private double eps;
	// 予想リターン計算のための乱数値
	private int tau;
	// 学習プロセスのリターン
	private double ret_l;
	// 学習プロセスの参照期間
	private int t_l;
	// 学習プロセスの定数
	private double k_l;
	// 学習プロセスの乱数値
	private double q_t;
	// エージェントの注文数
	private static int ORDER_NUM = 1;
	// 注文価格の最大値
	private static int PRICE_MAX = 100000;
	// エージェントの予想リターン
	private double expectedRet;
	// エージェントの予想価格
	private double expectedPrice;
	// エージェントの注文価格
	private double orderPrice;
	// ティックサイズによる調整後の注文価格
	private double adjustedNAOrderPrice;
	// 所持しているポジション
	// 買いポジション：+1
	// 売りポジション：-1
	private int position;
	// エージェントの買いと売りの判断
	private Flag.Trade tradeFlag;

	// 21/3/30
	//NA連続成行（売り）注文開始期
	private int cnt_mrkt_ordr_strt ;
	//NA連続成行（売り）注文期間
	private int cnt_mrkt_ordr_prd ;
	//NA連続成行（売り）注文確率
	private double cnt_mrkt_ordr_pr ;
	
	private static double commission[] = { 0 };

	// パラメータクラス
	Parameter param;
	// 乱数
	// Random rand;
	// より高品質な乱数
	private SplittableRandom rand;

	// コンストラクタ
	public NormalAgent(Parameter param, int agentId) {
		// パラメータクラス
		this.param = param;

		// 乱数 - シード値を指定して初期化
		long seed = param.getSeedForAgent(agentId);
		this.rand = new SplittableRandom(seed);

		// ファンダメンタル成分の重み
		this.w_fund = rand.nextDouble() * this.param.getW1_max();
		// テクニカル成分の重み
		this.w_tech = rand.nextDouble() * this.param.getW2_max();
		// シミュレーション中で不変の重み
		this.u = rand.nextDouble() * this.param.getU_max();
		// 予想リターン計算のための乱数値
		this.tau = rand.nextInt(this.param.getTau_max()-1) + 1;

		// 学習の際の参照期間
		this.t_l = param.getT_l();
		// 学習の際の定数
		this.k_l = param.getK_l();
		// ポジション
		this.position = 0;
		
		// 2021/3/30 
		//NA連続成行（売り）注文開始期
		this.cnt_mrkt_ordr_strt = this.param.getCnt_mrkt_ordr_strt() ;
		//NA連続成行（売り）注文期間
		this.cnt_mrkt_ordr_prd = this.param.getCnt_mrkt_ordr_prd() ;
		//NA連続成行（売り）注文確率
		this.cnt_mrkt_ordr_pr = this.param.getCnt_mrkt_ordr_pr() ;

	}

	// 注文の発注
	public Flag.Trade order(Market market, int time, int i) {
		// 直近の市場価格
		double marketPrice = market.getMarketPrice();
		
		// 予想リターンの計算
		this.expectedRet = this.calcExpectedRet(this.param.getSigma_e());
		// 2024/07/29
		// 分岐が必要なため、引数timeを追加
		// 予想価格の計算
		this.expectedPrice = this.calcExpectedPrice(marketPrice, i, time);
		// 注文価格の計算
		this.orderPrice = this.calcOrderPrice(this.param.getEst());
		// 注文価格のティックサイズによる調整
		this.adjustedNAOrderPrice = this.adjustOrderPrice(orderPrice, i);
		// 2024/09/03
		// 今回は、板形成期間は20000と仮定
		if (time < 20000) {
			// カウントしない
		} else {
			if (this.tradeFlag == Flag.Trade.SELL) {
				if (market.getBestBid() < this.adjustedNAOrderPrice) {
					market.updatelimitVolume();
				} else {
					market.updateinstantVolume();
				}
			} else if (this.tradeFlag == Flag.Trade.BUY) {
				if (market.getBestAsk() > this.adjustedNAOrderPrice) {
					market.updatelimitVolume();
				} else {
					market.updateinstantVolume();
				}
			}	
		}
		
		//  発注
		if(this.tradeFlag != Flag.Trade.NONE) {
			// 板形成期間の場合
			if(time <= this.param.getT_bookMake()) {
				this.tradeFlag = bookMakeOrderType(this.adjustedNAOrderPrice);
			}
			if(tradeFlag == Flag.Trade.NONE) {
				return this.tradeFlag; 
			}

			// 注文価格で発注
			Order order = new Order(this, time, this.adjustedNAOrderPrice, ORDER_NUM, this.tradeFlag);
			market.addOrder(order);
		}
		return this.tradeFlag;
	}

	// ファンダメンタル成分の予想リターンの計算
	private double calcExpectedW1Ret(double nMarketPrice) {
		double expectedW1Ret;

		// 予想リターンの計算
		expectedW1Ret = Math.log(this.param.getP_fund() / nMarketPrice);

		return expectedW1Ret;
	}

	// テクニカル成分の予想リターンの計算
	private double calcExpectedW2Ret(double nMarketPrice, double tauMarketPrice) {
		double expectedW2Ret;

		// 予想リターンの計算
		expectedW2Ret = Math.log(nMarketPrice / tauMarketPrice);

		return expectedW2Ret;
	}

	// 予想リターンの計算
	private double calcExpectedRet(double sigma_e) {
		// 予想リターン
		double exRet;
		
		this.eps = rand.nextGaussian() * sigma_e;

		// 予想リターンの計算
		// w1  :ファンダメンタル成分の重み
		// w2  :テクニカル成分の重み
		// u   :重みの最大値
		// ret1:ファンダメンタル投資家成分の予想リターン
		// ret2:テクニカル投資家成分の予想リターン
		// eps :正規分布乱数
		exRet = ((w_fund * ret_fund) + (w_tech * ret_tech) + (u * eps))/(w_fund + w_tech + u);

		return exRet;
	}

	// 予想価格の計算
	private double calcExpectedPrice(double marketPrice, int i, int time) {
		// 予想価格
		double expectedPrice;

		// 予想価格の計算
		// marketPrice:１期前の市場価格
		// ret        :予想リターン
		expectedPrice = marketPrice * Math.exp(this.expectedRet);
		
		return expectedPrice;
	}

	// 注文価格の計算
	private double calcOrderPrice(double est) {
		// 一様乱数を用いる
		// 予想価格 - pdiから予想価格 + pdiを注文価格に
		double pdi = 1000;
		double pd = rand.nextDouble() * (pdi*2) - pdi;
		orderPrice = this.expectedPrice + pd;

//以下追加実装　：注文価格計算に使う乱数を正規分布乱数から一様乱数に変更
		// 一様乱数を使用した注文価格計算
		// Random random = new Random();
		// 予想価格 - pdiから予想価格 + pdiを注文価格に
		// int pdi ;
		// pdi=1000;
		// int pd = random.nextInt(pdi*2)-pdi;
		// orderPrice = this.expectedPrice + pd;

		// 正規分布乱数を使用した注文価格計算
		// expectedPrice //:予想価格
		// est           //:ばらつき係数
		// double sigmaPrice //:標準偏差
		// sigmaPrice = this.expectedPrice * est;
		// orderPrice = this.expectedPrice + (rand.nextGaussian() * sigmaPrice);
//以上

		return orderPrice;
	}

	// 注文価格のティックサイズによる調整
	private double adjustOrderPrice(double orderPrice, int i) {
		// 暫定注文価格
		double price = orderPrice;
		// ティックサイズで割り切れる注文価格
		double adjustPrice;

		// 注文価格の最小最大値を設定
		if(orderPrice < param.getDelta_p()) {
			price = param.getDelta_p();
		}
		if(orderPrice > PRICE_MAX) {
			price = PRICE_MAX;
		}

		// 暫定注文価格より予想価格が低い場合
		if(this.expectedPrice < price) {
			adjustPrice = (price / (double)this.param.getDelta_p()) * this.param.getDelta_p();
			this.tradeFlag = Flag.Trade.SELL;
			
			// 2024/07/03
			// 手数料配列の利用
			// 2024/09/03
			// 注文価格時の補正に目的を絞る
			adjustPrice += commission[(i % commission.length)]; 
		}
		// 暫定注文価格より予想価格が高い場合
		else if (this.expectedPrice > price){
			adjustPrice = (price / (double)this.param.getDelta_p()) * this.param.getDelta_p();
			this.tradeFlag=Flag.Trade.BUY;
			
			// 2024/07/03
			// 手数料配列の利用
			// 2024/09/03
			// 注文価格時の補正に目的を絞る
			adjustPrice -= commission[(i % commission.length)];		
		}
		else {
			adjustPrice = 0;
			this.tradeFlag = Flag.Trade.NONE;
		}
		return adjustPrice;
	}

	// 学習期間の予想リターンの計算
	private double calcLearningRet(double marketPrice, double t_lMarketPrice) {
		double learnRet;

		// 予想リターンの計算
		learnRet = Math.log(marketPrice / t_lMarketPrice);

		return learnRet;
	}

	// 重みの更新
	// q_t: シミュレーション時刻
	public void updateWeight(Market market, int time) {

		// 確率mで重みの再設定
		if(rand.nextDouble() < this.param.getM()) {
			this.w_fund = rand.nextDouble() * this.param.getW1_max();
			this.w_tech = rand.nextDouble() * this.param.getW2_max();
			return;
		}

		// 学習期間のリターン
		this.ret_l = this.calcLearningRet(market.getMarketPrice(), market.getPreMarketPrice(this.t_l, time));
		// ファンダメンタル成分の予想リターン
		this.ret_fund = this.calcExpectedW1Ret(market.getPreMarketPrice(this.param.getN(), time));
		// テクニカル成分の予想リターン
		this.ret_tech = this.calcExpectedW2Ret(market.getPreMarketPrice(this.param.getN(), time), market.getPreMarketPrice(this.tau + this.param.getN(), time));
		// 時刻tでの一様乱数
		this.q_t = rand.nextDouble();

		// 25/04/06 学習機能を切る
		/*
		// ファンダメンタル成分と参照期間のリターンが同符号の場合
		if((this.ret_fund * this.ret_l) > 0) {
			this.w_fund = this.w_fund + (this.param.getK_l() * Math.abs(this.ret_l) * q_t * (this.param.getW1_max() - this.w_fund));
		}
		// ファンダメンタル成分と参照期間のリターンが異符号の場合
		else if((this.ret_fund * this.ret_l) < 0){
			this.w_fund = this.w_fund - (this.param.getK_l() * Math.abs(this.ret_l) * q_t * this.w_fund);
		}

		// テクニカル成分の予想リターンと参照期間のリターンが同符号の場合
		if((this.ret_tech * this.ret_l) > 0) {
			this.w_tech = this.w_tech + (this.param.getK_l() * Math.abs(this.ret_l) * q_t * (this.param.getW2_max() - this.w_tech));
		}
		// テクニカル成分の予想リターンと参照期間のリターンが異符号の場合
		else if((this.ret_tech * this.ret_l) < 0){
			this.w_tech = this.w_tech - (this.param.getK_l() * Math.abs(this.ret_l) * q_t * this.w_tech);
		}
		*/

		// 重みが最大値を超えていた場合，最大値に調整
		adjustWeight();
	}

	// 重みの値の調整
	private void adjustWeight() {
		// 重みが最大値を超えていた場合，最大値に設定
		if(this.w_fund > this.param.getW1_max()) {
			this.w_fund = this.param.getW1_max();
		}
		// 負の値なら０
		else if(this.w_fund < 0) {
			this.w_fund = 0;
		}
		if(this.w_tech > this.param.getW2_max()) {
			this.w_tech = this.param.getW2_max();
		}
		// 負の値なら０
		else if(this.w_tech < 0) {
			this.w_tech = 0;
		}
	}

	// 板形成期間の買いと売りの判断
	public Flag.Trade bookMakeOrderType(double orderPrice) {
		// 注文価格がファンダメンタル価格よりも高かった場合
		if(orderPrice >= this.param.getP_fund()-1) {
			return Flag.Trade.SELL;
		}
		// 注文価格がファンダメンタル価格よりも低かった場合
//		else if(orderPrice < this.param.getP_fund()){
//			return Flag.BUY;
//		}
		else {
//			return Flag.NONE;
			return Flag.Trade.BUY;
		}
	}


	// ポジションの更新
	@Override
	public void updatePosition(Flag.Trade tradeFlag, int orderVol) {
		// 買い注文：+1
		if(tradeFlag == Flag.Trade.BUY) {
			position = position + orderVol;
		}
		// 売り注文：-1
		else if(tradeFlag == Flag.Trade.SELL) {
			position = position - orderVol;
		}
	}

	public double getW1() {
		return this.w_fund;
	}
	public double getW2() {
		return this.w_tech;
	}
	public double getRet1() {
		return this.ret_fund;
	}
	public double getRet2() {
		return this.ret_tech;
	}
	//予想リターンを返す
	public double getExpectedRet() {
		return this.expectedRet;
	}
	//予想価格を返す
	public double getExpectedPrice() {
		return this.expectedPrice;
	}
	//注文価格を返す
	public double getAdjustedNAOrderPrice() {
		return this.adjustedNAOrderPrice;
	}
	//注文タイプを返す
	public Flag.Trade getTradeFlag() {
		return this.tradeFlag;
	}
}
