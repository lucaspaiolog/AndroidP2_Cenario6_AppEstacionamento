package br.edu.fatecpg.appestacionamento

import br.edu.fatecpg.appestacionamento.repository.AuthRepository
import br.edu.fatecpg.appestacionamento.repository.AuthRepositoryImpl
import br.edu.fatecpg.appestacionamento.repository.ParkingRepository
import br.edu.fatecpg.appestacionamento.repository.ParkingRepositoryImpl
import br.edu.fatecpg.appestacionamento.viewmodel.AdminViewModelFactory
import br.edu.fatecpg.appestacionamento.viewmodel.AuthViewModelFactory
import br.edu.fatecpg.appestacionamento.viewmodel.DriverViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Objeto simples para injeção de dependência (substitua por Hilt em projeto real)
object Injector {

    private fun getAuthRepository(): AuthRepository {
        return AuthRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
    }

    private fun getParkingRepository(): ParkingRepository {
        return ParkingRepositoryImpl(FirebaseFirestore.getInstance())
    }

    fun provideAuthViewModelFactory(): AuthViewModelFactory {
        return AuthViewModelFactory(getAuthRepository())
    }

    fun provideDriverViewModelFactory(): DriverViewModelFactory {
        return DriverViewModelFactory(getParkingRepository())
    }

    fun provideAdminViewModelFactory(): AdminViewModelFactory {
        return AdminViewModelFactory(getParkingRepository())
    }
}