import { create } from 'zustand';

interface BankKeyStore {
  bankPublicKeys: Record<string, string>;
  setBankPublicKey: (bankCode: string, publicKey: string) => void;
  getBankPublicKey: (bankCode: string) => string | null;
}

export const useBankKeyStore = create<BankKeyStore>((set, get) => ({
  bankPublicKeys: {},
  setBankPublicKey: (bankCode, publicKey) =>
    set((state) => ({
      bankPublicKeys: { ...state.bankPublicKeys, [bankCode]: publicKey },
    })),
  getBankPublicKey: (bankCode) => get().bankPublicKeys[bankCode] ?? null,
}));
