package thkaguyamod.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import thkaguyamod.spellcard.THSpellCard;

/**
 * スペルカードの登録に関するクラス
 *
 * <p>1.7.10 は {@code Class<? extends THSpellCard>} を保持して {@code Class.newInstance()} で
 * インスタンス化していた（リフレクション）。1.21.1 では {@link Supplier} ファクトリを登録し、
 * 生成は {@code factory.get()} で行う。name ↔ id ↔ domain のマッピングと並び替えロジックは旧版のまま。
 */
public class SpellCardRegistry
{
	public static Map<Integer, String> spellcardName = new HashMap<Integer, String>();
	public static Map<String, String> spellcardModDomain = new HashMap<String, String>();
	public static Map<String, Supplier<? extends THSpellCard>> spellcardFactory = new HashMap<String, Supplier<? extends THSpellCard>>();
	public static Map<String, Integer> spellcardID = new HashMap<String, Integer>();

	/**
	 * スペルカードを登録する。登録するファクトリはTHSpellCardを継承したものを返すこと
	 * @param factory         : THSpellCardを継承したインスタンスを生成するファクトリ。個別のスペルカード処理のみ書かれたもの
	 * @param modDomain       : 宣言したMODのドメイン名
	 * @param name            : スペルカードの登録名。符名抜きの日本語名をローマ字表記が望ましい　for example: "MusouFuuin", "Kasho_no_Eimin"
	 * @param id              : 他のスペルカードと被らない数字。128以上が望ましい。
	 * @return 登録に成功したらtrue
	 */
	public static boolean registerSpellCard(Supplier<? extends THSpellCard> factory, String modDomain, String name, int id)
	{
		if(idCheck(id))
		{
			spellcardName.put(id, name);
			spellcardModDomain.put(name, modDomain);
			spellcardFactory.put(name, factory);
			spellcardID.put(name, id);
			return true;
		}
		return false;
	}

	private static boolean idCheck(int id)
	{
		for(String name : spellcardID.keySet())
		{
			if(spellcardID.get(name) == id)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * スペルカードIDからそのスペルカードの登録名を返す
	 * @param id
	 * @return
	 */
	public static String getSpellCardName(int id)
	{
		return spellcardName.get(id);
	}

	/**
	 * スペルカードIDからそのスペルカードの属するModのドメイン名を返す
	 * @param name
	 * @return
	 */
	public static String getSpellCardModDomain(String name)
	{
		return spellcardModDomain.get(name);
	}

	/**
	 * スペルカードIDからそのスペルカードの属するModのドメイン名を返す
	 * @param name
	 * @return
	 */
	public static String getSpellCardModDomain(int id)
	{
		return spellcardModDomain.get(getSpellCardName(id));
	}

	/**
	 * 登録名からスペルカードIDを返す
	 * @param name
	 * @return
	 */
	public static int getSpellCardNumber(String name)
	{
		return spellcardID.get(name);
	}

	/**
	 * 登録名からスペルカードの処理の書かれたファクトリを返す
	 * @param name
	 * @return
	 */
	public static Supplier<? extends THSpellCard> getSpellCardFactory(String name)
	{
		return spellcardFactory.get(name);
	}

	/**
	 * スペルカードIDからスペルカードの処理の書かれたファクトリを返す
	 * @param id
	 * @return
	 */
	public static Supplier<? extends THSpellCard> getSpellCardFactory(int id)
	{
		return spellcardFactory.get(getSpellCardName(id));
	}

	/**
	 * 登録されているスペルカードの合計数を返す
	 * @return
	 */
	public static int getNumberOfSpellCard()
	{
		return spellcardID.size();
	}

	/**
	 * スペルカードのListを取得する
	 * @return
	 */
	public static List<String> getSpellCardList()
	{
		int length = getNumberOfSpellCard();//登録されているスペルカードの枚数
		String[] spellcardNames = new String[length + 1];//スペルカードの登録名の配列
		int[] spellcardUsers = new int[length + 1];//スペルカードの原作使用者IDの配列
		spellcardUsers[0] = 255;//スペルカードの原作使用者IDの配列の先頭の初期値を最大値にする
		spellcardNames[0] = "";
		THSpellCard spellcard;

		int userID;
		int keyNumber = 0;
		int keyNumber2 = 0;
		boolean success = true;

		//Map内の全てのキーを参照
		for(String name : spellcardID.keySet())
		{
			//キーからファクトリを取得し、インスタンスを生成
			Supplier<? extends THSpellCard> factory = spellcardFactory.get(name);
			if(factory != null && (spellcard = factory.get()) != null)
			{
				//原作使用者IDを取得
				userID = spellcard.getOriginalUserName();
				//spellcardUsers[keyNumber] = userID;
				//spellcardNames[keyNumber] = name;
				int i = 0;
				keyNumber++;
				keyNumber2 = keyNumber;
				//スペルカードの原作仕様者のIDの配列を参照
				do
				{
					//原作使用者IDが参照中のIDより小さいなら
					if(userID < spellcardUsers[i])
					{
						//現在のiより大きい配列の中身を全て一つ後ろに動かす
						for(int j = keyNumber2; j > i; j--)
						{
							spellcardUsers[j] = spellcardUsers[j - 1];
							spellcardNames[j] = spellcardNames[j - 1];
							keyNumber2--;
						}
						spellcardUsers[i] = userID;
						spellcardNames[i] = name;
					}
					i++;
				}
				while(i < keyNumber2);
			}
			else
			{
				//1.7.10 では newInstance() 失敗時に success = false としていた。ファクトリが無い/生成失敗も同義。
				success = false;
			}
		}

		if(success)
		{
			List<String> list = new ArrayList<String>();
			for(int i = 0; i < length; i++)
			{
				list.add(spellcardNames[i]);
			}
			return list;
		}
		return null;
	}

	/**
	 * スペルカードの原作の使用者IDを返す
	 * @param name スペルカード名
	 * @return
	 */
	public static int getSpellCardOriginalUser(String name)
	{
		Supplier<? extends THSpellCard> factory = getSpellCardFactory(name);
		if(factory != null)
		{
			THSpellCard useSpellCard = factory.get();
			if(useSpellCard != null)
			{
				return useSpellCard.getOriginalUserName();
			}
		}
		return -100;
	}


	public static List<String> sortSpellCard()
	{
		List<String> list = getSpellCardList();
		List<String> newList = new ArrayList<String>();
		String array[] = new String[256];
		array[0] = list.get(0);

		for(int i = 1; i < list.size(); i++)
		{
			if(getSpellCardOriginalUser(array[i - 1]) > getSpellCardOriginalUser(list.get(i)))
			{
				int j = i;
				while(j > 0 && getSpellCardOriginalUser(array[j - 1]) > getSpellCardOriginalUser(list.get(j)))
				{
					array[j] = array[j-1];
					j--;
				}
			}
			else
			{
				array[i] = list.get(i);
			}
		}

		for(int i = 0; i < list.size(); i++)
		{
			newList.add(array[i]);
		}
		return newList;
	}

}
