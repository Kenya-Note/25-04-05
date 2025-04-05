public class Flag {

  public enum HFT {
    NONE,	// HFTなし
    SMM,	// シンプルマーケットメイカー
    nPMM,	// ポジションマーケットメイカー（成行注文なし）
    mPMM,	// ポジションマーケットメイカー（成行注文あり）
    OMM,	// オーダーインバランスマーケットメイカー
    rOMM,	// オーダーインバランス（逆張り注文）マーケットメイカー
    POMM,	// nPMM & OMM
    PrOMM	// nPMM & rOMM
  }

  public enum Export {
    DATAFULLSET,
    DifferenceODI
  }

  // 売買種別を列挙型にて宣言
  public enum Trade {
    BUY,
    SELL,
    NONE
  }

  // プログラムの実行方法
  public enum Run {
    RELEASE,
    DEBUG,
  }

}
