package com.hotguy.workshopmanagement.task;

import com.hotguy.workshopmanagement.client.model.Client;
import com.hotguy.workshopmanagement.mechanic.model.Mechanic;
import com.hotguy.workshopmanagement.task.model.WorkshopTask;
import com.hotguy.workshopmanagement.vehicle.model.Vehicle;
import com.hotguy.workshopmanagement.vehicle.model.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios del modelo {@link WorkshopTask}.
 * Verifica la lógica de negocio encapsulada en la entidad:
 * ciclo de vida, cálculo de costes y progreso.
 */
@DisplayName("WorkshopTask - Tests del modelo")
class WorkshopTaskModelTest {

    private Client client;
    private Vehicle car;
    private Vehicle van;
    private Mechanic mechanic;
    private WorkshopTask task;

    @BeforeEach
    void setUp() {
        client = Client.builder()
            .id(1L).name("Test").surname1("Client").nif("12345678A")
            .email("test@test.com").clientCode(1).build();

        car = Vehicle.builder()
            .id(1L).registrationCode("TEST-001").model("Toyota").type(VehicleType.CAR)
            .proprietary(client).build();

        van = Vehicle.builder()
            .id(2L).registrationCode("TEST-002").model("Mercedes").type(VehicleType.VAN)
            .proprietary(client).build();

        mechanic = Mechanic.builder()
            .id(1L).name("Test").surname1("Mechanic").nif("87654321B")
            .email("mec@test.com").specialty("General").registrationDate(LocalDate.now()).build();

        task = WorkshopTask.builder()
            .id(1L).diagnostic("Test diagnosis").previewHours(4f)
            .initDate(LocalDate.now()).client(client).vehicle(car).mechanic(mechanic)
            .realHours(0f).finished(false).paid(false).build();
    }

    // ── Estado inicial ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Una tarea nueva debe estar en estado Pendiente")
    void newTaskShouldBePending() {
        assertThat(task.getStatus()).isEqualTo("Pendiente");
        assertThat(task.isFinished()).isFalse();
        assertThat(task.isPaid()).isFalse();
        assertThat(task.getRealHours()).isZero();
    }

    @Test
    @DisplayName("El coste total debe ser 0 si la tarea no está finalizada")
    void totalCostShouldBeZeroWhenNotFinished() {
        assertThat(task.getTotalCost()).isZero();
    }

    // ── Adición de horas ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Añadir horas debe acumularlas correctamente")
    void addingHoursShouldAccumulateThem() {
        task.addHours(2f);
        task.addHours(1.5f);
        assertThat(task.getRealHours()).isEqualTo(3.5f);
    }

    @Test
    @DisplayName("Añadir horas debe cambiar el estado a En progreso")
    void addingHoursShouldChangeStatusToInProgress() {
        task.addHours(1f);
        assertThat(task.getStatus()).isEqualTo("En progreso");
    }

    @Test
    @DisplayName("Añadir horas a tarea finalizada debe lanzar IllegalStateException")
    void addingHoursToFinishedTaskShouldThrow() {
        task.finish();
        assertThatThrownBy(() -> task.addHours(1f))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Añadir horas negativas debe lanzar IllegalArgumentException")
    void addingNegativeHoursShouldThrow() {
        assertThatThrownBy(() -> task.addHours(-1f))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Añadir cero horas debe lanzar IllegalArgumentException")
    void addingZeroHoursShouldThrow() {
        assertThatThrownBy(() -> task.addHours(0f))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Progreso ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Progreso debe ser 0% sin horas trabajadas")
    void progressShouldBeZeroWithoutHours() {
        assertThat(task.getProgress()).isZero();
    }

    @Test
    @DisplayName("Progreso debe calcular el porcentaje correctamente")
    void progressShouldCalculatePercentage() {
        task.addHours(1f); // 1 de 4 horas = 25%
        assertThat(task.getProgress()).isEqualTo(25f);
    }

    @Test
    @DisplayName("Progreso no debe superar el 100% aunque se excedan las horas estimadas")
    void progressShouldBeCappedAt100Percent() {
        task.addHours(10f); // 10 de 4 horas → capped al 100%
        assertThat(task.getProgress()).isEqualTo(100f);
    }

    // ── Finalización y coste ─────────────────────────────────────────────────

    @Test
    @DisplayName("Finalizar tarea debe cambiar el estado a Finalizada")
    void finishTaskShouldChangeStatus() {
        task.finish();
        assertThat(task.isFinished()).isTrue();
        assertThat(task.getStatus()).isEqualTo("Finalizada");
    }

    @Test
    @DisplayName("Coste de coche: 25€/hora × horas reales")
    void carTotalCostShouldBeCalculatedCorrectly() {
        task.addHours(4f);
        task.finish();
        // 4h × 25€/h + 0€ fijo = 100€
        assertThat(task.getTotalCost()).isEqualTo(100f);
    }

    @Test
    @DisplayName("Coste estimado de coche: 25€/hora × horas estimadas")
    void carEstimatedCostShouldBeCalculatedCorrectly() {
        // 4h × 25€/h + 0€ fijo = 100€
        assertThat(task.getEstimatedCost()).isEqualTo(100f);
    }

    @Test
    @DisplayName("Coste de furgoneta: 30€/hora + 30€ fijo")
    void vanTotalCostShouldIncludeFixedFee() {
        WorkshopTask vanTask = WorkshopTask.builder()
            .diagnostic("Van task").previewHours(3f)
            .initDate(LocalDate.now()).client(client).vehicle(van).mechanic(mechanic)
            .realHours(0f).finished(false).paid(false).build();

        vanTask.addHours(3f);
        vanTask.finish();
        // 3h × 30€/h + 30€ fijo = 120€
        assertThat(vanTask.getTotalCost()).isEqualTo(120f);
    }

    // ── Pago ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Marcar como pagada una tarea finalizada debe funcionar")
    void markingFinishedTaskAsPaidShouldWork() {
        task.addHours(4f);
        task.finish();
        task.markAsPaid();

        assertThat(task.isPaid()).isTrue();
        assertThat(task.getStatus()).isEqualTo("Pagada");
    }

    @Test
    @DisplayName("Marcar como pagada una tarea no finalizada debe lanzar excepción")
    void markingUnfinishedTaskAsPaidShouldThrow() {
        assertThatThrownBy(() -> task.markAsPaid())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Desmarcar pago debe cambiar isPaid a false")
    void markingAsUnpaidShouldWork() {
        task.finish();
        task.markAsPaid();
        task.markAsUnpaid();
        assertThat(task.isPaid()).isFalse();
    }

    // ── Flujo completo ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Flujo completo: Pendiente → En progreso → Finalizada → Pagada")
    void completeWorkflowShouldWork() {
        assertThat(task.getStatus()).isEqualTo("Pendiente");

        task.addHours(2f);
        assertThat(task.getStatus()).isEqualTo("En progreso");
        assertThat(task.getProgress()).isEqualTo(50f);

        task.addHours(2f);
        assertThat(task.getProgress()).isEqualTo(100f);

        task.finish();
        assertThat(task.getStatus()).isEqualTo("Finalizada");
        assertThat(task.getTotalCost()).isEqualTo(100f); // 4h × 25€

        task.markAsPaid();
        assertThat(task.getStatus()).isEqualTo("Pagada");
    }
}
