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

using Fis.Foundation.Vault;
using Fis.Foundation.Vault.Poc;

var builder = WebApplication.CreateBuilder(args);

// One client per process. Registering through a factory lets the container dispose it on shutdown,
// which revokes the token in AppRole mode.
builder.Services.AddSingleton(_ => VaultConfig.Load(
    Environment.GetEnvironmentVariable("VAULT_CONFIG_FILE") ?? Path.Combine(AppContext.BaseDirectory, "vault.json")));
builder.Services.AddSingleton<ISecretClient>(sp => SecretClient.Create(sp.GetRequiredService<VaultConfig>()));
builder.Services.AddSingleton(_ => PocSettings.FromEnvironment());

var app = builder.Build();

app.MapGet("/vault/health", async (ISecretClient client, PocSettings settings, CancellationToken ct) =>
{
    try
    {
        return Results.Ok(await PocCheck.RunAsync(client, settings, ct));
    }
    catch (VaultException ex)
    {
        return Results.Problem(title: ex.GetType().Name, detail: ex.Message, statusCode: StatusCodes.Status503ServiceUnavailable);
    }
});

app.Run();
