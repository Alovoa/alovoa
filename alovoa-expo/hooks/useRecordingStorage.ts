import { useState, useCallback, useEffect } from "react";
import { Platform } from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as FileSystem from "expo-file-system";
import { TranscriptSegment } from "./useTranscription";

export interface StoredRecording {
  id: string;
  title: string;
  createdAt: number;
  durationMs: number;
  audioUri: string;
  transcript: string;
  segments: TranscriptSegment[];
  markers: Array<{ id: string; positionMs: number; label?: string }>;
  // Generated content
  notes?: string[];
  flashcards?: Array<{ front: string; back: string }>;
  quiz?: Array<{
    question: string;
    options: string[];
    correctIndex: number;
  }>;
}

interface UseRecordingStorageReturn {
  recordings: StoredRecording[];
  isLoading: boolean;
  save: (recording: Omit<StoredRecording, "id" | "createdAt">) => Promise<string>;
  update: (id: string, updates: Partial<StoredRecording>) => Promise<void>;
  remove: (id: string) => Promise<void>;
  get: (id: string) => StoredRecording | undefined;
  clear: () => Promise<void>;
}

const STORAGE_KEY = "alovoa_recordings";
const DB_NAME = "alovoa_recordings_db";
const STORE_NAME = "recordings";

const getDocumentDirectory = (): string => {
  const fsAny = FileSystem as any;
  return fsAny.documentDirectory || fsAny.Paths?.document?.uri || "";
};

/**
 * Cross-platform recording storage hook.
 * Uses IndexedDB on web, AsyncStorage + FileSystem on native.
 *
 * Pattern: Store → persist recordings with metadata
 */
export function useRecordingStorage(): UseRecordingStorageReturn {
  const [recordings, setRecordings] = useState<StoredRecording[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Load recordings on mount
  useEffect(() => {
    loadRecordings();
  }, []);

  const loadRecordings = async () => {
    setIsLoading(true);
    try {
      if (Platform.OS === "web") {
        const data = await loadFromIndexedDB();
        setRecordings(data);
      } else {
        const data = await loadFromAsyncStorage();
        setRecordings(data);
      }
    } catch (error) {
      console.error("Failed to load recordings:", error);
    } finally {
      setIsLoading(false);
    }
  };

  // IndexedDB operations (web)
  const openDB = (): Promise<IDBDatabase> => {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, 1);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve(request.result);

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.createObjectStore(STORE_NAME, { keyPath: "id" });
        }
      };
    });
  };

  const loadFromIndexedDB = async (): Promise<StoredRecording[]> => {
    const db = await openDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, "readonly");
      const store = tx.objectStore(STORE_NAME);
      const request = store.getAll();

      request.onerror = () => reject(request.error);
      request.onsuccess = () => {
        const data = request.result as StoredRecording[];
        resolve(data.sort((a, b) => b.createdAt - a.createdAt));
      };
    });
  };

  const saveToIndexedDB = async (recording: StoredRecording): Promise<void> => {
    const db = await openDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, "readwrite");
      const store = tx.objectStore(STORE_NAME);
      const request = store.put(recording);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve();
    });
  };

  const deleteFromIndexedDB = async (id: string): Promise<void> => {
    const db = await openDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, "readwrite");
      const store = tx.objectStore(STORE_NAME);
      const request = store.delete(id);

      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve();
    });
  };

  const clearIndexedDB = async (): Promise<void> => {
    const db = await openDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, "readwrite");
      const store = tx.objectStore(STORE_NAME);
      const request = store.clear();

      request.onerror = () => reject(request.error);
      request.onsuccess = () => resolve();
    });
  };

  // AsyncStorage operations (native)
  const loadFromAsyncStorage = async (): Promise<StoredRecording[]> => {
    const json = await AsyncStorage.getItem(STORAGE_KEY);
    if (!json) return [];
    const data = JSON.parse(json) as StoredRecording[];
    return data.sort((a, b) => b.createdAt - a.createdAt);
  };

  const saveToAsyncStorage = async (data: StoredRecording[]): Promise<void> => {
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  };

  // Copy audio file to app's document directory (native only)
  const copyAudioToStorage = async (
    sourceUri: string,
    id: string
  ): Promise<string> => {
    if (Platform.OS === "web") {
      return sourceUri; // Blob URLs work directly on web
    }

    const documentDirectory = getDocumentDirectory();
    if (!documentDirectory) {
      throw new Error("No writable document directory available");
    }
    const destDir = `${documentDirectory}recordings/`;
    await FileSystem.makeDirectoryAsync(destDir, { intermediates: true });

    const extension = sourceUri.split(".").pop() || "m4a";
    const destUri = `${destDir}${id}.${extension}`;

    await FileSystem.copyAsync({ from: sourceUri, to: destUri });
    return destUri;
  };

  const save = useCallback(
    async (
      recording: Omit<StoredRecording, "id" | "createdAt">
    ): Promise<string> => {
      const id = `rec_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`;
      const createdAt = Date.now();

      // Copy audio to permanent storage
      const audioUri = await copyAudioToStorage(recording.audioUri, id);

      const newRecording: StoredRecording = {
        ...recording,
        id,
        createdAt,
        audioUri,
      };

      if (Platform.OS === "web") {
        await saveToIndexedDB(newRecording);
      } else {
        const current = await loadFromAsyncStorage();
        await saveToAsyncStorage([newRecording, ...current]);
      }

      setRecordings((prev) => [newRecording, ...prev]);
      return id;
    },
    []
  );

  const update = useCallback(
    async (id: string, updates: Partial<StoredRecording>): Promise<void> => {
      setRecordings((prev) => {
        const updated = prev.map((r) =>
          r.id === id ? { ...r, ...updates } : r
        );

        // Persist changes
        if (Platform.OS === "web") {
          const recording = updated.find((r) => r.id === id);
          if (recording) saveToIndexedDB(recording);
        } else {
          saveToAsyncStorage(updated);
        }

        return updated;
      });
    },
    []
  );

  const remove = useCallback(async (id: string): Promise<void> => {
    const recording = recordings.find((r) => r.id === id);

    if (Platform.OS === "web") {
      await deleteFromIndexedDB(id);
      // Revoke blob URL
      if (recording?.audioUri.startsWith("blob:")) {
        URL.revokeObjectURL(recording.audioUri);
      }
    } else {
      // Delete audio file
      if (recording?.audioUri) {
        await FileSystem.deleteAsync(recording.audioUri, {
          idempotent: true,
        }).catch(() => {});
      }
      const current = await loadFromAsyncStorage();
      await saveToAsyncStorage(current.filter((r) => r.id !== id));
    }

    setRecordings((prev) => prev.filter((r) => r.id !== id));
  }, [recordings]);

  const get = useCallback(
    (id: string): StoredRecording | undefined => {
      return recordings.find((r) => r.id === id);
    },
    [recordings]
  );

  const clear = useCallback(async (): Promise<void> => {
    if (Platform.OS === "web") {
      // Revoke all blob URLs
      recordings.forEach((r) => {
        if (r.audioUri.startsWith("blob:")) {
          URL.revokeObjectURL(r.audioUri);
        }
      });
      await clearIndexedDB();
    } else {
      // Delete all audio files
      const documentDirectory = getDocumentDirectory();
      const recordingsDir = `${documentDirectory}recordings/`;
      await FileSystem.deleteAsync(recordingsDir, { idempotent: true }).catch(
        () => {}
      );
      await AsyncStorage.removeItem(STORAGE_KEY);
    }

    setRecordings([]);
  }, [recordings]);

  return {
    recordings,
    isLoading,
    save,
    update,
    remove,
    get,
    clear,
  };
}
