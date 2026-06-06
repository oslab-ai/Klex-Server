"""
Thread-safe, in-memory query result cache with TTL support for Data Exploration.
"""
import time
import hashlib
import json
import logging
from collections import OrderedDict
from threading import Lock

logger = logging.getLogger(__name__)

class QueryCache:
    """
    An in-memory, thread-safe cache for database/CSV query results.
    Each adapter has its own LRU cache space.
    """
    def __init__(self, max_size=100, default_ttl=300):
        self.max_size = max_size
        self.default_ttl = default_ttl  # in seconds (default 5 minutes)
        self._lock = Lock()
        # Structure: { adapter_id: OrderedDict( { key_hash: (expires_at, result_data) } ) }
        self._caches = {}

    def _make_key(self, payload):
        """Generate a deterministic hash key for a query payload."""
        # Convert dictionary to deterministic JSON string by sorting keys
        payload_str = json.dumps(payload, sort_keys=True)
        return hashlib.sha256(payload_str.encode('utf-8')).hexdigest()

    def get(self, adapter_id, payload):
        """
        Retrieve a cached query result.
        Returns the result or None if missed or expired.
        """
        adapter_id = str(adapter_id)
        key = self._make_key(payload)
        now = time.time()

        with self._lock:
            if adapter_id not in self._caches:
                return None

            cache = self._caches[adapter_id]
            if key not in cache:
                return None

            expires_at, result = cache[key]

            # Check expiration
            if now > expires_at:
                del cache[key]
                return None

            # Move to end (MRU)
            cache.move_to_end(key)
            return result

    def set(self, adapter_id, payload, result, ttl=None):
        """
        Cache a query result.
        """
        adapter_id = str(adapter_id)
        key = self._make_key(payload)
        ttl = ttl if ttl is not None else self.default_ttl
        expires_at = time.time() + ttl

        with self._lock:
            if adapter_id not in self._caches:
                self._caches[adapter_id] = OrderedDict()

            cache = self._caches[adapter_id]

            # Insert or update
            cache[key] = (expires_at, result)
            cache.move_to_end(key)

            # Evict oldest if limit reached
            if len(cache) > self.max_size:
                cache.popitem(last=False)

    def invalidate_adapter(self, adapter_id):
        """
        Evict all cached entries for a specific adapter.
        """
        adapter_id = str(adapter_id)
        with self._lock:
            if adapter_id in self._caches:
                del self._caches[adapter_id]
                logger.info(f"Evicted query cache for adapter {adapter_id}")

    def clear_all(self):
        """
        Clear all caches.
        """
        with self._lock:
            self._caches.clear()
            logger.info("Cleared all query caches")


# Singleton instance
query_cache = QueryCache()
