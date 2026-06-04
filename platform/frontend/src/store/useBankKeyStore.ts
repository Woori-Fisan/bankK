import { create } from 'zustand';

interface BankKeyInfo {
  keyId: string;
  publicKey: string;
}

interface BankKeyStore {
  bankPublicKeys: Record<string, BankKeyInfo>;
  setBankPublicKey: (bankCode: string, keyInfo: BankKeyInfo) => void;
  setAllBankKeys: (keys: Record<string, BankKeyInfo>) => void;
  getBankPublicKey: (bankCode: string) => string | null;
  getBankKeyId: (bankCode: string) => string | null;
}

export const useBankKeyStore = create<BankKeyStore>((set, get) => ({
  bankPublicKeys: {},
  setBankPublicKey: (bankCode, keyInfo) =>
    set((state) => ({
      bankPublicKeys: { ...state.bankPublicKeys, [bankCode]: keyInfo },
    })),
  setAllBankKeys: (keys) => set({ bankPublicKeys: keys }),
  getBankPublicKey: (bankCode) => get().bankPublicKeys[bankCode]?.publicKey ?? null,
  getBankKeyId: (bankCode) => get().bankPublicKeys[bankCode]?.keyId ?? null,
}));
