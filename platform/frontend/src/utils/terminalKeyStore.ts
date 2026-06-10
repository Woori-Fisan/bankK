const DB_NAME = 'TerminalSecurityDB';
const DB_VERSION = 1;
const STORE_NAME = 'keys';
const KEY_ID = 'terminal_private_key';

/**
 * IndexedDB를 열고(필요시 생성) Promise를 반환합니다.
 */
const openDB = (): Promise<IDBDatabase> => {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);

        request.onupgradeneeded = (event) => {
            const db = (event.target as IDBOpenDBRequest).result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                db.createObjectStore(STORE_NAME);
            }
        };

        request.onsuccess = (event) => {
            resolve((event.target as IDBOpenDBRequest).result);
        };

        request.onerror = (event) => {
            reject((event.target as IDBOpenDBRequest).error);
        };
    });
};

/**
 * 단말기의 IndexedDB에 CryptoKey 객체를 저장합니다.
 */
export const saveTerminalPrivateKey = async (cryptoKey: CryptoKey): Promise<void> => {
    const db = await openDB();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction([STORE_NAME], 'readwrite');
        const store = transaction.objectStore(STORE_NAME);
        
        const request = store.put(cryptoKey, KEY_ID);

        request.onsuccess = () => resolve();
        request.onerror = (event) => reject((event.target as IDBRequest).error);
    });
};

/**
 * IndexedDB에서 저장된 CryptoKey 객체를 가져옵니다.
 */
export const getTerminalPrivateKey = async (): Promise<CryptoKey | null> => {
    try {
        const db = await openDB();
        return new Promise((resolve, reject) => {
            const transaction = db.transaction([STORE_NAME], 'readonly');
            const store = transaction.objectStore(STORE_NAME);
            const request = store.get(KEY_ID);

            request.onsuccess = () => {
                resolve(request.result || null);
            };
            request.onerror = (event) => reject((event.target as IDBRequest).error);
        });
    } catch (e) {
        console.error("IndexedDB 접근 실패:", e);
        return null;
    }
};

/**
 * 저장된 CryptoKey 객체를 삭제합니다.
 */
export const clearTerminalPrivateKey = async (): Promise<void> => {
    const db = await openDB();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction([STORE_NAME], 'readwrite');
        const store = transaction.objectStore(STORE_NAME);
        const request = store.delete(KEY_ID);

        request.onsuccess = () => resolve();
        request.onerror = (event) => reject((event.target as IDBRequest).error);
    });
};
