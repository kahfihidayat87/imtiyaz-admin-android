package com.imtiyaztour.admin

import android.util.Log

/**
 * Fase 3 D3: Algoritma auto-assign kamar hotel.
 *
 * Aturan prioritas:
 *  1. Keluarga (keluarga_group sama) → 1 kamar, tidak boleh dipecah (kecuali kapasitas kurang)
 *  2. Lansia + butuh kursi_roda → kamar lantai rendah + fasilitas kursi_roda_akses
 *  3. Lansia (usia >= 65) → kamar lantai rendah, diprioritaskan
 *  4. Gender — pria dengan pria, wanita dengan wanita (kecuali keluarga)
 *  5. Optimasi: minimize age gap dalam kamar (biar nyaman ngobrol)
 *  6. Sisa jamaah → pack by gender + usia serupa
 */
object RoomAssignEngine {

    private const val TAG = "RoomAssignEngine"
    private const val LANSIA_USIA = 65

    fun assign(
        jamaahList: List<JamaahManifest>,
        rooms: List<Room>,
        lansiaLantaiMaks: Int = 3
    ): AssignResult {
        if (jamaahList.isEmpty() || rooms.isEmpty()) {
            return AssignResult(
                unassigned = jamaahList,
                warnings = listOf(AssignWarning(type = "no_room", message = "Tidak ada kamar tersedia"))
            )
        }

        val warnings = mutableListOf<AssignWarning>()
        val availableRooms = rooms.toMutableList()
        // Penghuni per roomId
        val assignment = mutableMapOf<String, MutableList<JamaahManifest>>()
        availableRooms.forEach { assignment[it.id] = mutableListOf() }

        // ============ 1. Group by keluarga ============
        val keluargaGroups: Map<String, List<JamaahManifest>> = jamaahList
            .groupBy { it.keluarga_group.ifBlank { it.id } }

        // Sort: keluarga besar dulu (biar dapat kamar besar)
        val sortedGroups = keluargaGroups.values.sortedByDescending { it.size }

        val remaining = mutableListOf<JamaahManifest>()

        for (group in sortedGroups) {
            if (group.size == 1) {
                remaining.addAll(group)
                continue
            }
            // Cari kamar dengan kapasitas cukup
            val need = group.size
            val room = pickRoomForGroup(availableRooms, assignment, need)
            if (room != null) {
                assignment[room.id]?.addAll(group)
            } else {
                // Tidak ada kamar yang cukup — pecah
                warnings.add(AssignWarning(
                    type = "over_capacity",
                    jamaah_ids = group.map { it.id },
                    message = "Keluarga ${group.first().keluarga_group} (${group.size} orang) tidak dapat 1 kamar, akan dipecah"
                ))
                remaining.addAll(group)
            }
        }

        // ============ 2. Lansia + kursi_roda ============
        val lansiaKursiRoda = remaining.filter {
            it.usia >= LANSIA_USIA || it.kebutuhan_khusus.contains("kursi_roda")
        }.sortedByDescending { it.usia }
        lansiaKursiRoda.forEach { j ->
            if (isAssigned(assignment, j.id)) return@forEach
            val room = pickRoomForLansia(availableRooms, assignment, j, lansiaLantaiMaks)
            if (room != null) {
                assignment[room.id]?.add(j)
                remaining.remove(j)
            }
        }

        // ============ 3. Gender split sisa ============
        val sisaPria = remaining.filter { it.gender == "L" && !isAssigned(assignment, it.id) }
            .sortedByDescending { it.usia }
        val sisaWanita = remaining.filter { it.gender == "P" && !isAssigned(assignment, it.id) }
            .sortedByDescending { it.usia }
        val sisaUnknown = remaining.filter { it.gender != "L" && it.gender != "P" && !isAssigned(assignment, it.id) }

        fillByGender(assignment, availableRooms, sisaPria, "L")
        fillByGender(assignment, availableRooms, sisaWanita, "P")
        fillByGender(assignment, availableRooms, sisaUnknown, null)

        // ============ 4. Validasi ============
        val unassigned = jamaahList.filter { !isAssigned(assignment, it.id) }

        // Cek mixed gender tanpa keluarga
        assignment.forEach { (roomId, list) ->
            if (list.size >= 2) {
                val genders = list.map { it.gender }.toSet()
                val kelompok = list.map { it.keluarga_group.ifBlank { it.id } }.toSet()
                if (genders.size > 1 && kelompok.size > 1) {
                    warnings.add(AssignWarning(
                        type = "mixed_gender",
                        room_id = roomId,
                        jamaah_ids = list.map { it.id },
                        message = "Kamar $roomId berisi gender campuran tanpa ikatan keluarga"
                    ))
                }
            }
            val room = availableRooms.firstOrNull { it.id == roomId }
            if (room != null && list.size > room.kapasitas) {
                warnings.add(AssignWarning(
                    type = "over_capacity",
                    room_id = roomId,
                    jamaah_ids = list.map { it.id },
                    message = "Kamar ${room.nomor} over kapasitas (${list.size}/${room.kapasitas})"
                ))
            }
        }

        if (unassigned.isNotEmpty()) {
            warnings.add(AssignWarning(
                type = "no_room",
                jamaah_ids = unassigned.map { it.id },
                message = "${unassigned.size} jamaah belum dapat kamar (kamar kurang)"
            ))
        }

        Log.d(TAG, "assign done: ${assignment.values.sumOf { it.size }} assigned, ${unassigned.size} unassigned, ${warnings.size} warnings")

        return AssignResult(
            assignment = assignment.mapValues { it.value.toList() },
            unassigned = unassigned,
            warnings = warnings
        )
    }

    // ============ HELPERS ============

    private fun isAssigned(assignment: Map<String, MutableList<JamaahManifest>>, jamaahId: String): Boolean =
        assignment.values.any { list -> list.any { it.id == jamaahId } }

    /** Pilih kamar kosong (belum ada penghuni) dengan kapasitas >= need. */
    private fun pickRoomForGroup(
        rooms: List<Room>,
        assignment: Map<String, MutableList<JamaahManifest>>,
        need: Int
    ): Room? {
        return rooms
            .filter { (assignment[it.id]?.size ?: 0) == 0 && it.kapasitas >= need }
            .minByOrNull { it.kapasitas } // pilih yang paling pas (minimum waste)
    }

    /** Pilih kamar untuk lansia: lantai rendah, ada fasilitas kursi roda, gender cocok/kosong. */
    private fun pickRoomForLansia(
        rooms: List<Room>,
        assignment: Map<String, MutableList<JamaahManifest>>,
        jamaah: JamaahManifest,
        lantaiMaks: Int
    ): Room? {
        val butuhKursiRoda = jamaah.kebutuhan_khusus.contains("kursi_roda")
        val candidates = rooms.filter { room ->
            val occ = assignment[room.id] ?: return@filter false
            if (occ.size >= room.kapasitas) return@filter false
            // Gender cocok atau kosong
            val genderOk = occ.isEmpty() || occ.all { it.gender == jamaah.gender }
            // Lantai rendah (prioritas lansia)
            val lantaiOk = room.lantai <= lantaiMaks || room.kategori_prioritas == "lansia"
            // Fasilitas kursi roda
            val kursiOk = !butuhKursiRoda || room.fasilitas.contains("kursi_roda_akses")
            genderOk && lantaiOk && kursiOk
        }
        return candidates.minByOrNull { it.lantai }
    }

    /** Isi jamaah ke kamar kosong / sisa berdasarkan gender. */
    private fun fillByGender(
        assignment: MutableMap<String, MutableList<JamaahManifest>>,
        rooms: List<Room>,
        jamaahList: List<JamaahManifest>,
        gender: String?
    ) {
        var i = 0
        // Pass 1: kamar kosong
        for (room in rooms) {
            if (i >= jamaahList.size) return
            val occ = assignment[room.id] ?: continue
            if (occ.isNotEmpty()) continue
            if (gender != null && room.gender_lock != null && room.gender_lock != gender) continue
            while (i < jamaahList.size && occ.size < room.kapasitas) {
                occ.add(jamaahList[i])
                i++
            }
        }
        // Pass 2: kamar sisa (isi slot kosong)
        for (room in rooms) {
            if (i >= jamaahList.size) return
            val occ = assignment[room.id] ?: continue
            if (occ.size >= room.kapasitas) continue
            if (occ.isNotEmpty()) {
                // Gender harus sama
                if (gender != null && occ.any { it.gender != gender }) continue
            }
            while (i < jamaahList.size && occ.size < room.kapasitas) {
                occ.add(jamaahList[i])
                i++
            }
        }
    }
}
