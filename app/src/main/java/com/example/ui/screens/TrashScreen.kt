package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.LedgerViewModel

@Composable
fun TrashScreen(viewModel: LedgerViewModel, modifier: Modifier = Modifier) {
    val deletedAccounts by viewModel.allDeletedAccounts.collectAsState(initial = emptyList())
    val deletedTransactions by viewModel.allDeletedTransactions.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("سلة المحذوفات 🗑️", style = MaterialTheme.typography.titleLarge)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(deletedAccounts) { account ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(account.name)
                        Row {
                            IconButton(onClick = { viewModel.restoreAccount(account) }) {
                                Icon(Icons.Default.Restore, contentDescription = "استعادة")
                            }
                            IconButton(onClick = { viewModel.removeDeletedAccountPermanently(account.id) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "حذف نهائي")
                            }
                        }
                    }
                }
            }
            items(deletedTransactions) { tx ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tx.details)
                        Row {
                            IconButton(onClick = { viewModel.restoreTransaction(tx) }) {
                                Icon(Icons.Default.Restore, contentDescription = "استعادة")
                            }
                            IconButton(onClick = { viewModel.removeDeletedTransactionPermanently(tx.id) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "حذف نهائي")
                            }
                        }
                    }
                }
            }
        }
    }
}
