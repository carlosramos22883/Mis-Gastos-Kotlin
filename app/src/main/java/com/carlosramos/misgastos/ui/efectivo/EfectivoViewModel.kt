package com.carlosramos.misgastos.ui.efectivo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carlosramos.misgastos.data.remote.dto.CicloEfectivo
import com.carlosramos.misgastos.data.remote.dto.MovimientoEfectivo
import com.carlosramos.misgastos.data.remote.dto.MovimientoEfectivoBody
import com.carlosramos.misgastos.data.remote.dto.ResumenEfectivo
import com.carlosramos.misgastos.data.repository.Comunes
import com.carlosramos.misgastos.data.repository.FinanzasRepository
import com.carlosramos.misgastos.data.repository.Resultado
import com.carlosramos.misgastos.ui.common.FormMovimiento
import com.carlosramos.misgastos.ui.common.Formato
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EfectivoEstado(
    val cargando: Boolean = true,
    val cargandoMas: Boolean = false,
    val error: String? = null,
    val ciclo: CicloEfectivo? = null,
    val resumen: ResumenEfectivo? = null,
    val movimientos: List<MovimientoEfectivo> = emptyList(),
    val pagina: Int = 1,
    val hayMas: Boolean = false,
    /** null = todos · "ingreso" · "egreso" */
    val tipo: String? = null,
    /** Fecha dentro del ciclo consultado (null = el actual). */
    val cicloConsultado: String? = null,
    val comunes: Comunes? = null,
    val mensaje: String? = null
)

@HiltViewModel
class EfectivoViewModel @Inject constructor(private val repo: FinanzasRepository) : ViewModel() {
    private val _estado = MutableStateFlow(EfectivoEstado())
    val estado = _estado.asStateFlow()

    private val _form = MutableStateFlow<FormMovimiento?>(null)
    val form = _form.asStateFlow()

    init {
        viewModelScope.launch { (repo.comunes() as? Resultado.Ok)?.let { _estado.value = _estado.value.copy(comunes = it.datos) } }
        cargar()
    }

    fun cargar() = viewModelScope.launch {
        val e = _estado.value
        _estado.value = e.copy(cargando = e.movimientos.isEmpty(), error = null)
        when (val r = repo.efectivo(e.cicloConsultado, e.tipo, 1)) {
            is Resultado.Ok -> _estado.value = _estado.value.copy(
                cargando = false, ciclo = r.datos.ciclo, resumen = r.datos.resumen, movimientos = r.datos.data,
                pagina = 1, hayMas = r.datos.meta.paginaActual < r.datos.meta.ultimaPagina
            )
            is Resultado.Falla -> _estado.value = _estado.value.copy(cargando = false, error = r.mensaje)
        }
    }

    fun cargarMas() {
        val e = _estado.value
        if (!e.hayMas || e.cargandoMas) return
        _estado.value = e.copy(cargandoMas = true)
        viewModelScope.launch {
            when (val r = repo.efectivo(e.cicloConsultado, e.tipo, e.pagina + 1)) {
                is Resultado.Ok -> _estado.value = _estado.value.copy(
                    cargandoMas = false, movimientos = _estado.value.movimientos + r.datos.data,
                    pagina = r.datos.meta.paginaActual, hayMas = r.datos.meta.paginaActual < r.datos.meta.ultimaPagina
                )
                is Resultado.Falla -> _estado.value = _estado.value.copy(cargandoMas = false, mensaje = r.mensaje)
            }
        }
    }

    fun filtrar(tipo: String?) { _estado.value = _estado.value.copy(tipo = tipo, movimientos = emptyList()); cargar() }

    fun irACiclo(fecha: String?) { _estado.value = _estado.value.copy(cicloConsultado = fecha, movimientos = emptyList()); cargar() }

    fun mensajeVisto() { _estado.value = _estado.value.copy(mensaje = null) }

    fun avisar(texto: String) { _estado.value = _estado.value.copy(mensaje = texto) }

    // ----- Formulario -----

    fun nuevo() {
        val yo = _estado.value.comunes?.usuarioId
        _form.value = FormMovimiento(userId = yo, fecha = minOf(Formato.hoy(), _estado.value.ciclo?.fin ?: Formato.hoy()))
    }

    fun editar(m: MovimientoEfectivo) {
        if (!m.editable) {
            _estado.value = _estado.value.copy(
                mensaje = if (m.compromisoId != null) "Es el pago de un compromiso: se modifica desde Compromisos."
                else "Este movimiento se modifica desde su módulo (deudas, préstamos, ingresos o transferencias)."
            )
            return
        }
        _form.value = FormMovimiento(
            id = m.id, tipo = m.tipo, descripcion = m.descripcion, monto = m.monto, fecha = m.fecha,
            categoriaId = m.categoria?.id, userId = m.userId, contactoId = m.contacto?.id
        )
    }

    fun cambiar(f: FormMovimiento) { _form.value = f }

    fun cerrarForm() { _form.value = null }

    fun guardar() {
        val f = _form.value ?: return
        val errores = buildMap {
            if (f.descripcion.isBlank()) put("descripcion", "Escribe una descripción.")
            if (!Formato.montoValido(f.monto)) put("monto", "Monto positivo con máximo dos decimales.")
            if (f.categoriaId == null) put("categoria_personal_id", "Elige una categoría.")
        }
        if (errores.isNotEmpty()) { _form.value = f.copy(errores = errores); return }
        _form.value = f.copy(guardando = true, errores = emptyMap(), error = null)
        viewModelScope.launch {
            val body = MovimientoEfectivoBody(f.tipo, f.descripcion.trim(), f.monto, f.fecha, f.categoriaId, f.userId, f.contactoId?.toString())
            when (val r = repo.guardarEfectivo(f.id, body)) {
                is Resultado.Ok -> {
                    _form.value = null
                    _estado.value = _estado.value.copy(mensaje = if (f.id == null) "Movimiento registrado." else "Movimiento actualizado.")
                    cargar()
                }
                is Resultado.Falla -> _form.value = f.copy(guardando = false, errores = r.campos, error = r.mensaje)
            }
        }
    }

    fun eliminar() {
        val id = _form.value?.id ?: return
        _form.value = _form.value?.copy(guardando = true)
        viewModelScope.launch {
            when (val r = repo.eliminarEfectivo(id)) {
                is Resultado.Ok -> { _form.value = null; _estado.value = _estado.value.copy(mensaje = "Movimiento eliminado."); cargar() }
                is Resultado.Falla -> _form.value = _form.value?.copy(guardando = false, error = r.mensaje)
            }
        }
    }
}
