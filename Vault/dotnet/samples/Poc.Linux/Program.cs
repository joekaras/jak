//Copyright Notice!
//This document is protected under the trade secret and copyright
//laws as the property of Fidelity National Information Services, Inc.
//Copying, reproduction or distribution should be limited and only to
//employees with a "need to know" to do their job.
//Any disclosure of this document to third parties is strictly prohibited.

//copyright 2026 Fidelity National Information Services.
//All rights reserved worldwide.

// This file contains code that was generated in part with the assistance of Claude Code,
// an AI Coding Assistant. Portions of this file were written by the developer.

// Runs under systemd with vault-poc.service, or as a console app for local tries.
// This POC defaults to AppRole with a wrapped secret_id; see vault.json.

using Fis.Foundation.Vault;
using Fis.Foundation.Vault.Poc;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

var builder = Host.CreateApplicationBuilder(args);
builder.Services.AddSystemd();

builder.Services.AddSingleton(_ => VaultConfig.Load(
    Environment.GetEnvironmentVariable("VAULT_CONFIG_FILE") ?? Path.Combine(AppContext.BaseDirectory, "vault.json")));
builder.Services.AddSingleton<ISecretClient>(sp => SecretClient.Create(sp.GetRequiredService<VaultConfig>()));
builder.Services.AddSingleton(_ => PocSettings.FromEnvironment());
builder.Services.AddHostedService<PocWorker>();

builder.Build().Run();
